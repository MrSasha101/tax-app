package ua.lpnu.tax.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static Connection connection;

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            Properties props = loadProperties("db.properties");
            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String password = props.getProperty("db.password");
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    private static Properties loadProperties(String fileName) {
        Properties props = new Properties();
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) throw new RuntimeException("Unable to find " + fileName);
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database properties", e);
        }
        return props;
    }
}