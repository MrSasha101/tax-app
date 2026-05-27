package ua.lpnu.tax.dao;

import ua.lpnu.tax.model.*;
import ua.lpnu.tax.model.income.*;
import ua.lpnu.tax.model.tax.Tax;
import ua.lpnu.tax.service.TaxCalculationService;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class IncomeDao implements CrudDao<Income> {

    private final TaxCalculationService taxService = new TaxCalculationService();
    private final TaxDao taxDao = new TaxDao();

    @Override
    public Income findById(int id) throws SQLException {
        String sql = "SELECT i.*, it.name as type_name, it.default_tax_rate " +
                "FROM incomes i JOIN income_types it ON i.type_id = it.id WHERE i.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Income income = createIncomeFromRow(rs);
                loadBenefits(income);
                loadTaxes(income);
                return income;
            }
        }
        return null;
    }

    public List<Income> findByTaxpayerId(int taxpayerId) throws SQLException {
        List<Income> list = new ArrayList<>();
        String sql = "SELECT i.*, it.name as type_name, it.default_tax_rate " +
                "FROM incomes i JOIN income_types it ON i.type_id = it.id " +
                "WHERE i.taxpayer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taxpayerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Income income = createIncomeFromRow(rs);
                loadBenefits(income);
                loadTaxes(income);
                list.add(income);
            }
        }
        return list;
    }

    @Override
    public List<Income> findAll() throws SQLException {
        List<Income> list = new ArrayList<>();
        String sql = "SELECT i.*, it.name as type_name, it.default_tax_rate " +
                "FROM incomes i JOIN income_types it ON i.type_id = it.id";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Income income = createIncomeFromRow(rs);
                loadBenefits(income);
                loadTaxes(income);
                list.add(income);
            }
        }
        return list;
    }

    @Override
    public Income save(Income entity) throws SQLException {
        // Розрахунок податків перед збереженням
        List<Tax> taxes = taxService.calculateTaxes(entity);
        entity.setCalculatedTaxes(taxes);

        String sql = "INSERT INTO incomes (taxpayer_id, type_id, amount, source, accrual_date) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, entity.getTaxpayer().getId());
            stmt.setInt(2, entity.getType().getId());
            stmt.setBigDecimal(3, entity.getAmount());
            stmt.setString(4, entity.getSource());
            stmt.setDate(5, Date.valueOf(entity.getAccrualDate()));
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                entity.setId(keys.getInt(1));
            }
        }
        // Зберегти податки
        for (Tax tax : taxes) {
            tax.setIncome(entity);
            taxDao.save(tax);
        }
        // Зберегти зв'язки з пільгами
        saveBenefits(entity);
        return entity;
    }

    @Override
    public Income update(Income entity) throws SQLException {
        String sql = "UPDATE incomes SET amount=?, source=?, accrual_date=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, entity.getAmount());
            stmt.setString(2, entity.getSource());
            stmt.setDate(3, Date.valueOf(entity.getAccrualDate()));
            stmt.setInt(4, entity.getId());
            stmt.executeUpdate();
        }
        // Оновити податки
        taxDao.deleteByIncomeId(entity.getId());
        List<Tax> taxes = taxService.calculateTaxes(entity);
        entity.setCalculatedTaxes(taxes);
        for (Tax tax : taxes) {
            tax.setIncome(entity);
            taxDao.save(tax);
        }
        // Оновити пільги
        deleteBenefits(entity.getId());
        saveBenefits(entity);
        return entity;
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM incomes WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private Income createIncomeFromRow(ResultSet rs) throws SQLException {
        String typeName = rs.getString("type_name");
        IncomeType type = new IncomeType();
        type.setId(rs.getInt("type_id"));
        type.setName(typeName);
        type.setDefaultTaxRate(rs.getBigDecimal("default_tax_rate"));

        Taxpayer taxpayer = new Taxpayer();
        taxpayer.setId(rs.getInt("taxpayer_id"));

        BigDecimal amount = rs.getBigDecimal("amount");
        String source = rs.getString("source");
        LocalDate date = rs.getDate("accrual_date").toLocalDate();

        Income income;
        switch (typeName) {
            case "Заробітна плата":
                income = new SalaryIncome();
                break;
            case "Авторська винагорода":
                income = new AuthorRoyaltyIncome();
                break;
            case "Продаж нерухомого майна (перший продаж)":
                income = new PropertySaleIncome();
                break;
            case "Грошовий подарунок від родичів I ступеня":
                income = new GiftIncome();
                ((GiftIncome) income).setFromFirstDegreeRelative(true);
                break;
            case "Грошовий подарунок (інші)":
                income = new GiftIncome();
                ((GiftIncome) income).setFromFirstDegreeRelative(false);
                break;
            case "Переказ з-за кордону":
                income = new ForeignTransferIncome();
                break;
            case "Матеріальна допомога":
                income = new MaterialAidIncome();
                break;
            default:
                income = new SalaryIncome(); // fallback
        }
        income.setId(rs.getInt("id"));
        income.setTaxpayer(taxpayer);
        income.setType(type);
        income.setAmount(amount);
        income.setSource(source);
        income.setAccrualDate(date);
        return income;
    }

    private void loadBenefits(Income income) throws SQLException {
        String sql = "SELECT b.* FROM benefits b " +
                "JOIN income_benefits ib ON b.id = ib.benefit_id " +
                "WHERE ib.income_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, income.getId());
            ResultSet rs = stmt.executeQuery();
            List<Benefit> benefits = new ArrayList<>();
            while (rs.next()) {
                Benefit b = new Benefit();
                b.setId(rs.getInt("id"));
                b.setName(rs.getString("name"));
                b.setDescription(rs.getString("description"));
                b.setDiscountAmount(rs.getBigDecimal("discount_amount"));
                b.setDiscountPercentage(rs.getBigDecimal("discount_percentage"));
                benefits.add(b);
            }
            income.setAppliedBenefits(benefits);
        }
    }

    private void loadTaxes(Income income) throws SQLException {
        income.setCalculatedTaxes(taxDao.findByIncomeId(income.getId()));
    }

    private void saveBenefits(Income income) throws SQLException {
        String sql = "INSERT INTO income_benefits (income_id, benefit_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Benefit b : income.getAppliedBenefits()) {
                stmt.setInt(1, income.getId());
                stmt.setInt(2, b.getId());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void deleteBenefits(int incomeId) throws SQLException {
        String sql = "DELETE FROM income_benefits WHERE income_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, incomeId);
            stmt.executeUpdate();
        }
    }
}