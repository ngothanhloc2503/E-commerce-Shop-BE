package com.store.ecommerce.util;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.store.ecommerce.entity.SettingBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MailUtil {
    private static final Logger log = LoggerFactory.getLogger(MailUtil.class);

    public static void sendEmail(SettingBag emailSettings, String to, String subject, String content) {
        String apiKey = emailSettings.getValue("SENDGRID_API_KEY");
        String fromEmail = emailSettings.getValue("MAIL_FROM");
        String senderName = emailSettings.getValue("MAIL_SENDER_NAME");

        Email from = new Email(fromEmail, senderName);
        Email toEmail = new Email(to);
        Content htmlContent = new Content("text/html", content);
        Mail mail = new Mail(from, subject, toEmail, htmlContent);

        SendGrid sg = new SendGrid(apiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            log.info("SendGrid status: {}", response.getStatusCode());
            log.info("Response body: {}", response.getBody());
            log.info("Response headers: {}", response.getHeaders());
        } catch (IOException ex) {
            log.error("Error sending email", ex);
            throw new RuntimeException("❌ Error sending email: " + ex.getMessage());
        }
    }
}
