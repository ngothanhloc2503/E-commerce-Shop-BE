package com.store.ecommerce.util;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.store.ecommerce.entity.SettingBag;

import java.io.IOException;

public class MailUtil {
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

            System.out.println("SendGrid status: " + response.getStatusCode());
            System.out.println("Response body: " + response.getBody());
            System.out.println("Response headers: " + response.getHeaders());
        } catch (IOException ex) {
            throw new RuntimeException("❌ Error sending email: " + ex.getMessage());
        }
    }
}
