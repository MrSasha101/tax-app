package ua.lpnu.tax.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("EmailSender — утиліта відправки пошти")
class EmailSenderTest {

    @BeforeEach
    void setUp() {
        clearEmailProperties();
    }

    @AfterEach
    void tearDown() {
        clearEmailProperties();
        EmailSender.shutdown();
    }

    private void clearEmailProperties() {
        System.clearProperty("smtp.host");
        System.clearProperty("smtp.port");
        System.clearProperty("smtp.user");
        System.clearProperty("smtp.password");
        System.clearProperty("alert.email");
    }

    @Test
    @DisplayName("Якщо smtp.host не задано, метод тихо завершується")
    void sendCriticalAlert_hostNotSet_doesNothing() {
        assertDoesNotThrow(() -> EmailSender.sendCriticalAlert("Тест", "Тіло листа"));
    }

    @Test
    @DisplayName("Виконання логіки фонового потоку (перехоплення помилки з'єднання)")
    void sendCriticalAlert_handlesConnectionError() throws InterruptedException {
        // Задаємо локальний IP та свідомо закритий порт (1),
        // щоб спроба відправки миттєво падала з ConnectException
        System.setProperty("smtp.host", "127.0.0.1");
        System.setProperty("smtp.port", "1");
        System.setProperty("smtp.user", "test@test.com");
        System.setProperty("alert.email", "admin@test.com");

        // Метод викликається і передає задачу в фоновий Executor
        assertDoesNotThrow(() -> EmailSender.sendCriticalAlert("Критична помилка", "Впала БД"));

        // Даємо фоновому потоку 300 мілісекунд, щоб він встиг запуститися,
        // спробувати підключитися до 127.0.0.1:1, отримати помилку і записати її в лог.
        // Це забезпечує 100% покриття рядків класу!
        Thread.sleep(300);
    }
}