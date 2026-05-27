package ua.lpnu.tax.util;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Надсилає e-mail повідомлення при критичних помилках.
 * Запускається асинхронно у фоновому потоці — не блокує UI.
 *
 * Параметри SMTP передаються через системні properties:
 *   -Dsmtp.host=smtp.gmail.com
 *   -Dsmtp.port=587
 *   -Dsmtp.user=your@gmail.com
 *   -Dsmtp.password=app-password
 *   -Dalert.email=admin@example.com
 */
public class EmailSender {

    private static final Logger logger = LogManager.getLogger(EmailSender.class);

    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "email-sender");
        t.setDaemon(true);
        return t;
    });

    private EmailSender() {}

    /**
     * Асинхронно надсилає email про критичну помилку.
     * Якщо smtp.host не задано — тихо пропускає.
     *
     * @param subject тема листа
     * @param body    текст листа
     */
    public static void sendCriticalAlert(String subject, String body) {
        String host = System.getProperty("smtp.host");
        if (host == null || host.isBlank()) {
            logger.debug("SMTP не налаштовано, email не надсилається.");
            return;
        }

        executor.submit(() -> {
            try {
                String port     = System.getProperty("smtp.port",     "587");
                String user     = System.getProperty("smtp.user",     "");
                String password = System.getProperty("smtp.password", "");
                String to       = System.getProperty("alert.email",   user);

                Properties props = new Properties();
                props.put("mail.smtp.host",            host);
                props.put("mail.smtp.port",            port);
                props.put("mail.smtp.auth",            "true");
                props.put("mail.smtp.starttls.enable", "true");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(user, password);
                    }
                });

                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(user));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                msg.setSubject("[TaxApp] " + subject);
                msg.setText(body);
                Transport.send(msg);

                logger.info("Email-сповіщення надіслано на {}", to);
            } catch (Exception e) {
                logger.warn("Не вдалося надіслати email: {}", e.getMessage());
            }
        });
    }

    /** Викликається при завершенні програми. */
    public static void shutdown() {
        executor.shutdown();
    }
}
