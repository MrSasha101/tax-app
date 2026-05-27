package ua.lpnu.tax.dao;

import ua.lpnu.tax.model.Benefit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BenefitDao implements CrudDao<Benefit> {

    @Override
    public Benefit findById(int id) throws SQLException {
        String sql = "SELECT * FROM benefits WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null;
    }

    @Override
    public List<Benefit> findAll() throws SQLException {
        List<Benefit> list = new ArrayList<>();
        String sql = "SELECT * FROM benefits";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public Benefit save(Benefit entity) throws SQLException {
        String sql = "INSERT INTO benefits (name, description, discount_amount, discount_percentage) " +
                "VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, entity.getName());
            stmt.setString(2, entity.getDescription());
            stmt.setBigDecimal(3, entity.getDiscountAmount());
            stmt.setBigDecimal(4, entity.getDiscountPercentage());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                entity.setId(keys.getInt(1));
            }
        }
        return entity;
    }

    @Override
    public Benefit update(Benefit entity) throws SQLException {
        String sql = "UPDATE benefits SET name=?, description=?, discount_amount=?, discount_percentage=? " +
                "WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entity.getName());
            stmt.setString(2, entity.getDescription());
            stmt.setBigDecimal(3, entity.getDiscountAmount());
            stmt.setBigDecimal(4, entity.getDiscountPercentage());
            stmt.setInt(5, entity.getId());
            stmt.executeUpdate();
        }
        return entity;
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM benefits WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private Benefit mapRow(ResultSet rs) throws SQLException {
        Benefit b = new Benefit();
        b.setId(rs.getInt("id"));
        b.setName(rs.getString("name"));
        b.setDescription(rs.getString("description"));
        b.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        b.setDiscountPercentage(rs.getBigDecimal("discount_percentage"));
        return b;
    }
}