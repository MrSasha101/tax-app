package ua.lpnu.tax;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ua.lpnu.tax.util.EmailSender;

public class MainApp extends Application {

    private static final Logger logger = LogManager.getLogger(MainApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Запуск Tax Management Application...");
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/MainView.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);
        primaryStage.setTitle("Податковий облік фізичної особи");
        primaryStage.setScene(scene);
        primaryStage.show();
        logger.info("Головне вікно відкрито.");
    }

    @Override
    public void stop() {
        logger.info("Завершення програми.");
        EmailSender.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
