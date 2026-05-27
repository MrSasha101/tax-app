package ua.lpnu.tax.dao;

import java.sql.SQLException;
import java.util.List;

public interface CrudDao<T> {
    T findById(int id) throws SQLException;
    List<T> findAll() throws SQLException;
    T save(T entity) throws SQLException;
    T update(T entity) throws SQLException;
    void delete(int id) throws SQLException;
}