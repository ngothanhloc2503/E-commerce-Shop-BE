package com.store.ecommerce.util;

import com.store.ecommerce.entity.SettingBag;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

public class MailUtil {
    public static JavaMailSenderImpl prepareMailSender(SettingBag emailSettings) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(emailSettings.getValue("MAIL_HOST"));
        mailSender.setPort(Integer.parseInt(emailSettings.getValue("MAIL_PORT")));
        mailSender.setUsername(emailSettings.getValue("MAIL_USERNAME"));
        mailSender.setPassword(emailSettings.getValue("MAIL_PASSWORD"));

        Properties mailProperties = new Properties();
        mailProperties.setProperty("mail.smtp.auth", emailSettings.getValue("SMTP_AUTH"));
//        mailProperties.setProperty("mail.smtp.starttls.enable", emailSettings.getValue("SMTP_SECURED"));
        String secured = emailSettings.getValue("SMTP_SECURED");
        if ("true".equals(secured)) {
            mailProperties.setProperty("mail.smtp.starttls.enable", "true");  // Turn on STARTTLS
        } else {
            mailProperties.setProperty("mail.smtp.ssl.enable", "true");  // Turn on SSL if not using STARTTLS
        }

        mailSender.setJavaMailProperties(mailProperties);

        return mailSender;
    }
}
