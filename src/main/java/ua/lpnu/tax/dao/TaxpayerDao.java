package ua.lpnu.tax.dao;

import ua.lpnu.tax.model.Taxpayer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TaxpayerDao implements CrudDao<Taxpayer> {

    @Override
    public Taxpayer findById(int id) throws SQLException {
        String sql = "SELECT * FROM taxpayers WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    @Override
    public List<Taxpayer> findAll() throws SQLException {
        List<Taxpayer> list = new ArrayList<>();
        String sql = "SELECT * FROM taxpayers";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    @Override
    public Taxpayer save(Taxpayer entity) throws SQLException {
        String sql = "INSERT INTO taxpayers (full_name, tax_id, children_count, special_status) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, entity.getFullName());
            stmt.setString(2, entity.getTaxId());
            stmt.setInt(3, entity.getChildrenCount());
            stmt.setString(4, entity.getSpecialStatus());
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) entity.setId(keys.getInt(1));
        }
        return entity;
    }

    @Override
    public Taxpayer update(Taxpayer entity) throws SQLException {
        String sql = "UPDATE taxpayers SET full_name=?, tax_id=?, children_count=?, special_status=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entity.getFullName());
            stmt.setString(2, entity.getTaxId());
            stmt.setInt(3, entity.getChildrenCount());
            stmt.setString(4, entity.getSpecialStatus());
            stmt.setInt(5, entity.getId());
            stmt.executeUpdate();
        }
        return entity;
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM taxpayers WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private Taxpayer mapRow(ResultSet rs) throws SQLException {
        Taxpayer tp = new Taxpayer();
        tp.setId(rs.getInt("id"));
        tp.setFullName(rs.getString("full_name"));
        tp.setTaxId(rs.getString("tax_id"));
        tp.setChildrenCount(rs.getInt("children_count"));
        tp.setSpecialStatus(rs.getString("special_status"));
        return tp;
    }
}