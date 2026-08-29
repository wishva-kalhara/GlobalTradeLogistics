package me.wishva.globalTradeLogistics.notificationSvc.mail;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import me.wishva.globalTradeLogistics.core.configs.AppConfig;
import me.wishva.globalTradeLogistics.core.dto.EmailNotification;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends a rendered {@link EmailNotification} over SMTP, built from
 * {@link AppConfig#SMTP_HOST}/{@link AppConfig#SMTP_PORT}/{@link AppConfig#SMTP_AUTH}/
 * {@link AppConfig#SMTP_EMAIL}/{@link AppConfig#SMTP_PASSWORD}. A plain
 * {@code jakarta.mail.Session} is built by hand (not a GlassFish mail-session
 * JNDI resource) so the SMTP account is configurable purely through those
 * env vars, with no extra asadmin provisioning step.
 * <p>
 * A missing {@link AppConfig#SMTP_HOST} or a send failure is logged and
 * swallowed rather than thrown — this runs inside an MDB's {@code onMessage},
 * and letting the exception escape would redeliver the same message forever.
 */
public final class EmailSenderService {

    private static final Logger LOG = Logger.getLogger(EmailSenderService.class.getName());

    private EmailSenderService() {
    }

    public static void send(EmailNotification notification) {
        if (AppConfig.SMTP_HOST.isBlank()) {
            LOG.warning(() -> "SMTP_HOST is not configured — skipping email send. type=" + notification.getType()
                    + " recipientEmail=" + notification.getRecipientEmail());
            return;
        }

        try {
            MimeMessage message = new MimeMessage(buildSession());
            message.setFrom(new InternetAddress(AppConfig.SMTP_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(notification.getRecipientEmail()));
            message.setSubject(EmailTemplateService.subjectFor(notification), "UTF-8");
            message.setContent(EmailTemplateService.renderHtml(notification), "text/html; charset=UTF-8");

            Transport.send(message);
            LOG.info(() -> "Sent " + notification.getType() + " email to " + notification.getRecipientEmail());
        } catch (MessagingException e) {
            LOG.log(Level.SEVERE, "Failed to send " + notification.getType()
                    + " email to " + notification.getRecipientEmail(), e);
        }
    }

    private static Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", AppConfig.SMTP_HOST);
        props.put("mail.smtp.port", AppConfig.SMTP_PORT);
        props.put("mail.smtp.auth", String.valueOf(AppConfig.SMTP_AUTH));
//        props.put("mail.smtp.starttls.enable", "true");

        if (!AppConfig.SMTP_AUTH) {
            return Session.getInstance(props);
        }

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(AppConfig.SMTP_EMAIL, AppConfig.SMTP_PASSWORD);
            }
        });
    }
}
