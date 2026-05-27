package ua.lpnu.tax.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.lpnu.tax.dao.IncomeDao;
import ua.lpnu.tax.dao.TaxpayerDao;
import ua.lpnu.tax.model.Taxpayer;
import ua.lpnu.tax.model.income.Income;
import ua.lpnu.tax.model.tax.Tax;
import ua.lpnu.tax.util.EmailSender;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportController {

    private static final Logger logger = LogManager.getLogger(ReportController.class);

    @FXML private ComboBox<Taxpayer> cmbTaxpayer;
    @FXML private ComboBox<Integer>  cmbYear;
    @FXML private TextArea           reportArea;
    @FXML private Label              lblTotalIncome;
    @FXML private Label              lblTotalTax;

    private final TaxpayerDao taxpayerDao = new TaxpayerDao();
    private final IncomeDao   incomeDao   = new IncomeDao();

    @FXML
    public void initialize() {
        // Заповнити список платників
        try {
            List<Taxpayer> all = taxpayerDao.findAll();
            cmbTaxpayer.setItems(FXCollections.observableArrayList(all));
        } catch (SQLException e) {
            logger.error("Помилка завантаження платників у ReportController", e);
        }

        // Роки — поточний і 5 попередніх
        int cur = LocalDate.now().getYear();
        List<Integer> years = new ArrayList<>();
        for (int y = cur; y >= cur - 5; y--) years.add(y);
        cmbYear.setItems(FXCollections.observableArrayList(years));
        cmbYear.getSelectionModel().selectFirst();

        logger.debug("ReportController ініціалізовано");
    }

    @FXML
    private void onGenerateReport() {
        Taxpayer taxpayer = cmbTaxpayer.getValue();
        Integer  year     = cmbYear.getValue();
        if (taxpayer == null || year == null) {
            new Alert(Alert.AlertType.WARNING, "Виберіть платника і рік.", ButtonType.OK).showAndWait();
            return;
        }
        logger.info("Формування звіту: платник={} (id={}), рік={}", taxpayer.getFullName(), taxpayer.getId(), year);

        try {
            List<Income> incomes = incomeDao.findByTaxpayerId(taxpayer.getId());
            // Фільтрація по роках
            List<Income> yearIncomes = incomes.stream()
                    .filter(i -> i.getAccrualDate().getYear() == year)
                    .toList();

            BigDecimal totalIncome = yearIncomes.stream()
                    .map(Income::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPdfo = yearIncomes.stream()
                    .flatMap(i -> i.getCalculatedTaxes().stream())
                    .filter(t -> "ПДФО".equals(t.getTaxName()))
                    .map(Tax::getTaxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalMilitary = yearIncomes.stream()
                    .flatMap(i -> i.getCalculatedTaxes().stream())
                    .filter(t -> "Військовий збір".equals(t.getTaxName()))
                    .map(Tax::getTaxAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalTax = totalPdfo.add(totalMilitary);

            lblTotalIncome.setText("%,.2f грн".formatted(totalIncome));
            lblTotalTax.setText("%,.2f грн".formatted(totalTax));

            // Формуємо текстовий звіт
            StringBuilder sb = new StringBuilder();
            sb.append("═".repeat(60)).append("\n");
            sb.append("  ЗВЕДЕНИЙ ЗВІТ ЗА %d РІК\n".formatted(year));
            sb.append("  Платник: %s\n".formatted(taxpayer.getFullName()));
            sb.append("  ІПН: %s\n".formatted(taxpayer.getTaxId()));
            sb.append("═".repeat(60)).append("\n\n");
            sb.append("ДОХОДИ:\n").append("─".repeat(60)).append("\n");
            for (Income inc : yearIncomes) {
                sb.append("  %-35s %12.2f грн%n"
                        .formatted(inc.getType().getName(), inc.getAmount()));
            }
            sb.append("─".repeat(60)).append("\n");
            sb.append("  %-35s %12.2f грн%n".formatted("Разом доходів:", totalIncome));
            sb.append("\nНАРАХОВАНІ ПОДАТКИ:\n").append("─".repeat(60)).append("\n");
            sb.append("  %-35s %12.2f грн%n".formatted("ПДФО:", totalPdfo));
            sb.append("  %-35s %12.2f грн%n".formatted("Військовий збір:", totalMilitary));
            sb.append("─".repeat(60)).append("\n");
            sb.append("  %-35s %12.2f грн%n".formatted("РАЗОМ до сплати:", totalTax));
            sb.append("═".repeat(60)).append("\n");

            reportArea.setText(sb.toString());
            logger.info("Звіт сформовано: дохід={}, податки={}", totalIncome, totalTax);

        } catch (SQLException e) {
            logger.error("Помилка формування звіту", e);
            EmailSender.sendCriticalAlert("Помилка звіту", e.getMessage());
            new Alert(Alert.AlertType.ERROR, "Помилка: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void onSaveReport() {
        String text = reportArea.getText();
        if (text == null || text.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Спочатку сформуйте звіт.", ButtonType.OK).showAndWait();
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Зберегти звіт");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстовий файл (*.txt)", "*.txt"));
        fc.setInitialFileName("tax_report_%d.txt".formatted(cmbYear.getValue()));
        File file = fc.showSaveDialog(reportArea.getScene().getWindow());
        if (file == null) return;
        try {
            Files.writeString(file.toPath(), text);
            logger.info("Звіт збережено у файл: {}", file.getAbsolutePath());
            new Alert(Alert.AlertType.INFORMATION, "Збережено:\n" + file.getAbsolutePath(), ButtonType.OK)
                    .showAndWait();
        } catch (IOException e) {
            logger.error("Помилка збереження звіту", e);
            new Alert(Alert.AlertType.ERROR, "Помилка: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }
}
