package ua.lpnu.tax.dao;

import ua.lpnu.tax.model.tax.MilitaryLevy;
import ua.lpnu.tax.model.tax.PersonalIncomeTax;
import ua.lpnu.tax.model.tax.Tax;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaxDao implements CrudDao<Tax> {
    @Override
    public Tax findById(int id) throws SQLException {
        // реалізація
        return null;
    }

    @Override
    public List<Tax> findAll() throws SQLException {
        return null;
    }

    public List<Tax> findByIncomeId(int incomeId) throws SQLException {
        List<Tax> list = new ArrayList<>();
        String sql = "SELECT * FROM taxes WHERE income_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, incomeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Tax tax = createTaxFromRow(rs);
                list.add(tax);
            }
        }
        return list;
    }

    @Override
    public Tax save(Tax entity) throws SQLException {
        String sql = "INSERT INTO taxes (income_id, tax_name, tax_amount, rate_at_time) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, entity.getIncome().getId());
            stmt.setString(2, entity.getTaxName());
            stmt.setBigDecimal(3, entity.getTaxAmount());
            stmt.setBigDecimal(4, entity.getRateAtTime());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) entity.setId(keys.getInt(1));
        }
        return entity;
    }

    @Override
    public Tax update(Tax entity) throws SQLException {
        return entity;
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM taxes WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public void deleteByIncomeId(int incomeId) throws SQLException {
        String sql = "DELETE FROM taxes WHERE income_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, incomeId);
            stmt.executeUpdate();
        }
    }

    private Tax createTaxFromRow(ResultSet rs) throws SQLException {
        String taxName = rs.getString("tax_name");
        Tax tax;
        if ("ПДФО".equals(taxName)) {
            tax = new PersonalIncomeTax();
        } else {
            tax = new MilitaryLevy();
        }
        tax.setId(rs.getInt("id"));
        tax.setTaxName(taxName);
        tax.setTaxAmount(rs.getBigDecimal("tax_amount"));
        tax.setRateAtTime(rs.getBigDecimal("rate_at_time"));
        // income встановлюється пізніше
        return tax;
    }
}