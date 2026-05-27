package ua.lpnu.tax.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.lpnu.tax.dao.IncomeDao;
import ua.lpnu.tax.dao.IncomeTypeDao;
import ua.lpnu.tax.model.IncomeType;
import ua.lpnu.tax.model.Taxpayer;
import ua.lpnu.tax.model.income.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class IncomeDialogController {
    private static final Logger logger = LogManager.getLogger(IncomeDialogController.class);

    @FXML private ComboBox<IncomeType> cmbType;
    @FXML private TextField txtAmount;
    @FXML private TextField txtSource;
    @FXML private DatePicker dpDate;

    private Taxpayer taxpayer;
    private Income income;
    private boolean saved = false;
    private final IncomeDao incomeDao = new IncomeDao();
    private final IncomeTypeDao typeDao = new IncomeTypeDao();

    @FXML
    public void initialize() {
        try {
            cmbType.setItems(FXCollections.observableArrayList(typeDao.findAll()));
        } catch (SQLException e) {
            logger.error("Failed to load income types", e);
            showAlert("Помилка", "Не вдалося завантажити типи доходів.");
        }
        dpDate.setValue(LocalDate.now());
    }

    public void setTaxpayer(Taxpayer taxpayer) {
        this.taxpayer = taxpayer;
    }

    public void setIncome(Income income) {
        this.income = income;
        if (income != null) {
            cmbType.setValue(income.getType());
            txtAmount.setText(income.getAmount().toString());
            txtSource.setText(income.getSource());
            dpDate.setValue(income.getAccrualDate());
        }
    }

    @FXML
    private void onSave() {
        // Валідація
        IncomeType type = cmbType.getValue();
        if (type == null) {
            showAlert("Помилка", "Виберіть тип доходу.");
            return;
        }

        String amountText = txtAmount.getText().trim();
        if (amountText.isEmpty()) {
            showAlert("Помилка", "Введіть суму доходу.");
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert("Помилка", "Сума має бути більше нуля.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Помилка", "Невірний формат суми. Використовуйте цифри та крапку.");
            return;
        }

        String source = txtSource.getText().trim();
        if (source.isEmpty()) {
            showAlert("Помилка", "Введіть джерело доходу.");
            return;
        }

        LocalDate date = dpDate.getValue();
        if (date == null) {
            showAlert("Помилка", "Виберіть дату.");
            return;
        }

        if (income == null) {
            income = createIncomeByType(type, amount, source, date);
        } else {
            income.setType(type); // дозволяємо змінити тип
            income.setAmount(amount);
            income.setSource(source);
            income.setAccrualDate(date);
        }

        try {
            if (income.getId() == 0) {
                incomeDao.save(income);
            } else {
                incomeDao.update(income);
            }
            saved = true;
            close();
        } catch (SQLException e) {
            logger.error("Помилка збереження доходу", e);
            ua.lpnu.tax.util.EmailSender.sendCriticalAlert("Помилка збереження доходу", e.getMessage());
            showAlert("Помилка", "Не вдалося зберегти дохід: " + e.getMessage());
        }
    }

    private Income createIncomeByType(IncomeType type, BigDecimal amount, String source, LocalDate date) {
        String typeName = type.getName();
        Income inc;
        switch (typeName) {
            case "Заробітна плата":
                inc = new SalaryIncome(taxpayer, type, amount, source, date);
                break;
            case "Авторська винагорода":
                inc = new AuthorRoyaltyIncome(taxpayer, type, amount, source, date);
                break;
            case "Продаж нерухомого майна (перший продаж)":
                inc = new PropertySaleIncome(taxpayer, type, amount, source, date);
                break;
            case "Грошовий подарунок від родичів I ступеня":
                inc = new GiftIncome(taxpayer, type, amount, source, date, true);
                break;
            case "Грошовий подарунок (інші)":
                inc = new GiftIncome(taxpayer, type, amount, source, date, false);
                break;
            case "Переказ з-за кордону":
                inc = new ForeignTransferIncome(taxpayer, type, amount, source, date);
                break;
            case "Матеріальна допомога":
                inc = new MaterialAidIncome(taxpayer, type, amount, source, date, BigDecimal.ZERO);
                break;
            default:
                inc = new SalaryIncome(taxpayer, type, amount, source, date);
        }
        return inc;
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) cmbType.getScene().getWindow();
        stage.close();
    }

    public boolean isSaved() {
        return saved;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}