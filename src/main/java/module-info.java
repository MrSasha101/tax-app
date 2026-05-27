module ua.lpnu.tax {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.postgresql.jdbc;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires jakarta.mail;
    requires jakarta.activation;

    opens ua.lpnu.tax to javafx.fxml;
    opens ua.lpnu.tax.controller to javafx.fxml;
    opens ua.lpnu.tax.model to javafx.base;

    exports ua.lpnu.tax;
    exports ua.lpnu.tax.model;
    exports ua.lpnu.tax.model.income;
    exports ua.lpnu.tax.model.tax;
    exports ua.lpnu.tax.service;
    exports ua.lpnu.tax.dao;
    exports ua.lpnu.tax.util;
}
