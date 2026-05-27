package ua.lpnu.tax.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import ua.lpnu.tax.model.Taxpayer;

public class TaxpayerDialogController {
    @FXML private TextField txtFullName;
    @FXML private TextField txtTaxId;
    @FXML private TextField txtChildrenCount;
    @FXML private ComboBox<String> cmbSpecialStatus;

    private Taxpayer taxpayer;
    private boolean saved = false;

    @FXML
    public void initialize() {
        cmbSpecialStatus.getItems().addAll(
                "Немає",
                "Особа з інвалідністю I групи",
                "Особа з інвалідністю II групи",
                "Особа з інвалідністю III групи",
                "Учасник бойових дій",
                "Чорнобилець",
                "Багатодітний"
        );
        cmbSpecialStatus.setValue("Немає");
    }

    public void setTaxpayer(Taxpayer taxpayer) {
        this.taxpayer = taxpayer;
        txtFullName.setText(taxpayer.getFullName() != null ? taxpayer.getFullName() : "");
        txtTaxId.setText(taxpayer.getTaxId() != null ? taxpayer.getTaxId() : "");
        txtChildrenCount.setText(String.valueOf(taxpayer.getChildrenCount()));
        if (taxpayer.getSpecialStatus() != null && !taxpayer.getSpecialStatus().isEmpty()) {
            cmbSpecialStatus.setValue(taxpayer.getSpecialStatus());
        } else {
            cmbSpecialStatus.setValue("Немає");
        }
    }

    @FXML
    private void onSave() {
        // Валідація
        if (txtFullName.getText().trim().isEmpty()) {
            showAlert("Помилка", "Введіть ПІБ.");
            return;
        }
        if (txtTaxId.getText().trim().isEmpty()) {
            showAlert("Помилка", "Введіть ІПН.");
            return;
        }
        if (!txtTaxId.getText().trim().matches("\\d{10}")) {
            showAlert("Помилка", "ІПН має містити 10 цифр.");
            return;
        }
        int children;
        try {
            children = Integer.parseInt(txtChildrenCount.getText().trim());
            if (children < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert("Помилка", "Кількість дітей має бути цілим невід'ємним числом.");
            return;
        }

        taxpayer.setFullName(txtFullName.getText().trim());
        taxpayer.setTaxId(txtTaxId.getText().trim());
        taxpayer.setChildrenCount(children);
        taxpayer.setSpecialStatus(cmbSpecialStatus.getValue());
        saved = true;
        close();
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) txtFullName.getScene().getWindow();
        stage.close();
    }

    public boolean isSaved() {
        return saved;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}