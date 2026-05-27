package ua.lpnu.tax.dao;

import ua.lpnu.tax.model.IncomeType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IncomeTypeDao implements CrudDao<IncomeType> {

    @Override
    public IncomeType findById(int id) throws SQLException {
        String sql = "SELECT * FROM income_types WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    @Override
    public List<IncomeType> findAll() throws SQLException {
        List<IncomeType> list = new ArrayList<>();
        String sql = "SELECT * FROM income_types";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public IncomeType save(IncomeType entity) throws SQLException {
        String sql = "INSERT INTO income_types (name, default_tax_rate) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, entity.getName());
            stmt.setBigDecimal(2, entity.getDefaultTaxRate());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) entity.setId(keys.getInt(1));
        }
        return entity;
    }

    @Override
    public IncomeType update(IncomeType entity) throws SQLException {
        String sql = "UPDATE income_types SET name=?, default_tax_rate=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entity.getName());
            stmt.setBigDecimal(2, entity.getDefaultTaxRate());
            stmt.setInt(3, entity.getId());
            stmt.executeUpdate();
        }
        return entity;
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM income_types WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private IncomeType mapRow(ResultSet rs) throws SQLException {
        IncomeType it = new IncomeType();
        it.setId(rs.getInt("id"));
        it.setName(rs.getString("name"));
        it.setDefaultTaxRate(rs.getBigDecimal("default_tax_rate"));
        return it;
    }
}