package ua.lpnu.tax.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.lpnu.tax.dao.IncomeDao;
import ua.lpnu.tax.dao.TaxpayerDao;
import ua.lpnu.tax.model.Taxpayer;
import ua.lpnu.tax.model.income.Income;
import ua.lpnu.tax.util.EmailSender;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class MainController {

    private static final Logger logger = LogManager.getLogger(MainController.class);

    @FXML private ListView<Taxpayer> taxpayerListView;
    @FXML private TableView<Income>  incomeTable;
    @FXML private TableColumn<Income, String> colSource;
    @FXML private TableColumn<Income, String> colAmount;
    @FXML private TableColumn<Income, String> colDate;
    @FXML private Label lblTotalIncome;
    @FXML private Label lblTotalTax;

    private final TaxpayerDao taxpayerDao = new TaxpayerDao();
    private final IncomeDao   incomeDao   = new IncomeDao();

    @FXML
    public void initialize() {
        logger.info("Ініціалізація головного контролера");

        colSource.setCellValueFactory(new PropertyValueFactory<>("source"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("accrualDate"));

        // ── Налаштування відображення платників ──────────────────────────────
        taxpayerListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Taxpayer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getFullName());
                    setTooltip(new Tooltip("ІПН: " + item.getTaxId()
                            + "  |  Дітей: " + item.getChildrenCount()
                            + (item.getSpecialStatus() != null && !item.getSpecialStatus().equals("Немає")
                            ? "  |  " + item.getSpecialStatus() : "")));
                }
            }
        });

        refreshTaxpayerList();

        taxpayerListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> onTaxpayerSelected());
    }

    // ── Taxpayer operations ────────────────────────────────────────────────────

    private void refreshTaxpayerList() {
        try {
            List<Taxpayer> taxpayers = taxpayerDao.findAll();
            taxpayerListView.setItems(FXCollections.observableArrayList(taxpayers));
            logger.info("Завантажено {} платників", taxpayers.size());
        } catch (SQLException e) {
            logger.error("Помилка завантаження платників", e);
            EmailSender.sendCriticalAlert("Помилка БД", "Не вдалося завантажити платників:\n" + e.getMessage());
            showAlert("Помилка", "Не вдалося завантажити список платників.\n" + e.getMessage());
        }
    }

    private void onTaxpayerSelected() {
        Taxpayer selected = taxpayerListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            incomeTable.getItems().clear();
            lblTotalIncome.setText("0.00");
            lblTotalTax.setText("0.00");
            return;
        }
        logger.info("Вибрано платника: {} (id={})", selected.getFullName(), selected.getId());
        try {
            List<Income> incomes = incomeDao.findByTaxpayerId(selected.getId());
            selected.setIncomes(incomes);
            incomeTable.setItems(FXCollections.observableArrayList(incomes));
            lblTotalIncome.setText(selected.getTotalAnnualIncome().toPlainString());
            lblTotalTax.setText(selected.getTotalTaxesPaid().toPlainString());
            logger.debug("Завантажено {} доходів, загальний дохід={}, податки={}",
                    incomes.size(), selected.getTotalAnnualIncome(), selected.getTotalTaxesPaid());
        } catch (SQLException e) {
            logger.error("Помилка завантаження доходів для платника id={}", selected.getId(), e);
            EmailSender.sendCriticalAlert("Помилка БД", "Не вдалося завантажити доходи:\n" + e.getMessage());
            showAlert("Помилка", "Не вдалося завантажити доходи.");
        }
    }

    @FXML
    private void onAddTaxpayer() {
        logger.info("Відкриття діалогу додавання платника");
        Taxpayer newTaxpayer = new Taxpayer();
        boolean ok = showTaxpayerDialog(newTaxpayer);
        if (ok) {
            try {
                taxpayerDao.save(newTaxpayer);
                logger.info("Платника збережено: {}", newTaxpayer.getFullName());
                refreshTaxpayerList();
            } catch (SQLException e) {
                logger.error("Помилка збереження платника", e);
                EmailSender.sendCriticalAlert("Помилка збереження", e.getMessage());
                showAlert("Помилка", "Не вдалося зберегти платника:\n" + e.getMessage());
            }
        }
    }

    @FXML
    private void onEditTaxpayer() {
        Taxpayer selected = taxpayerListView.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Увага", "Виберіть платника для редагування."); return; }
        logger.info("Редагування платника id={}", selected.getId());
        boolean ok = showTaxpayerDialog(selected);
        if (ok) {
            try {
                taxpayerDao.update(selected);
                logger.info("Платника оновлено: {}", selected.getFullName());
                refreshTaxpayerList();
                onTaxpayerSelected();
            } catch (SQLException e) {
                logger.error("Помилка оновлення платника id={}", selected.getId(), e);
                EmailSender.sendCriticalAlert("Помилка оновлення", e.getMessage());
                showAlert("Помилка", "Не вдалося оновити платника:\n" + e.getMessage());
            }
        }
    }

    @FXML
    private void onDeleteTaxpayer() {
        Taxpayer selected = taxpayerListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Видалити \"" + selected.getFullName() + "\" та всі його доходи?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Підтвердження видалення");
        if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
            try {
                taxpayerDao.delete(selected.getId());
                logger.info("Платника видалено: id={}", selected.getId());
                refreshTaxpayerList();
                incomeTable.getItems().clear();
                lblTotalIncome.setText("0.00");
                lblTotalTax.setText("0.00");
            } catch (SQLException e) {
                logger.error("Помилка видалення платника id={}", selected.getId(), e);
                EmailSender.sendCriticalAlert("Помилка видалення", e.getMessage());
                showAlert("Помилка", "Не вдалося видалити платника:\n" + e.getMessage());
            }
        }
    }

    // ── Income operations ──────────────────────────────────────────────────────

    @FXML
    private void onAddIncome() {
        Taxpayer selected = taxpayerListView.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Увага", "Спочатку виберіть платника."); return; }
        logger.info("Відкриття діалогу додавання доходу для платника id={}", selected.getId());
        if (showIncomeDialog(null, selected)) onTaxpayerSelected();
    }

    @FXML
    private void onEditIncome() {
        Income selectedIncome = incomeTable.getSelectionModel().getSelectedItem();
        if (selectedIncome == null) { showAlert("Увага", "Виберіть дохід для редагування."); return; }
        Taxpayer selectedTaxpayer = taxpayerListView.getSelectionModel().getSelectedItem();
        logger.info("Редагування доходу id={}", selectedIncome.getId());
        if (showIncomeDialog(selectedIncome, selectedTaxpayer)) onTaxpayerSelected();
    }

    @FXML
    private void onDeleteIncome() {
        Income selectedIncome = incomeTable.getSelectionModel().getSelectedItem();
        if (selectedIncome == null) { showAlert("Увага", "Виберіть дохід для видалення."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Видалити дохід на суму " + selectedIncome.getAmount() + " грн?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Підтвердження видалення");
        if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
            try {
                incomeDao.delete(selectedIncome.getId());
                logger.info("Дохід id={} видалено", selectedIncome.getId());
                onTaxpayerSelected();
            } catch (SQLException e) {
                logger.error("Помилка видалення доходу id={}", selectedIncome.getId(), e);
                EmailSender.sendCriticalAlert("Помилка видалення доходу", e.getMessage());
                showAlert("Помилка", "Не вдалося видалити дохід:\n" + e.getMessage());
            }
        }
    }

    // ── Report operations ─────────────────────────────────────────────────────

    @FXML
    private void handleOpenReport() {
        Taxpayer selected = taxpayerListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Увага", "Будь ласка, спочатку виберіть платника зі списку зліва для формування звіту.");
            return;
        }

        try {
            logger.info("Відкриття вікна річного звіту для платника id={}", selected.getId());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReportView.fxml"));

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Річний податковий звіт — " + selected.getFullName());
            stage.setScene(new Scene(loader.load()));

            // Вікно звіту відображається поверх основного
            stage.showAndWait();

        } catch (IOException e) {
            logger.error("Помилка відкриття вікна звітів", e);
            showAlert("Помилка", "Не вдалося відкрити вікно річного звіту:\n" + e.getMessage());
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private boolean showTaxpayerDialog(Taxpayer taxpayer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TaxpayerDialog.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(taxpayer.getId() == 0 ? "Новий платник" : "Редагування платника");
            stage.setScene(new Scene(loader.load()));
            TaxpayerDialogController controller = loader.getController();
            controller.setTaxpayer(taxpayer);
            stage.showAndWait();
            return controller.isSaved();
        } catch (IOException e) {
            logger.error("Помилка відкриття діалогу платника", e);
            showAlert("Помилка", "Не вдалося відкрити вікно редагування.");
            return false;
        }
    }

    private boolean showIncomeDialog(Income income, Taxpayer taxpayer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/IncomeDialog.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(income == null ? "Новий дохід" : "Редагування доходу");
            stage.setScene(new Scene(loader.load()));
            IncomeDialogController controller = loader.getController();
            controller.setTaxpayer(taxpayer);
            if (income != null) controller.setIncome(income);
            stage.showAndWait();
            return controller.isSaved();
        } catch (IOException e) {
            logger.error("Помилка відкриття діалогу доходу", e);
            showAlert("Помилка", "Не вдалося відкрити вікно доходу.");
            return false;
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}