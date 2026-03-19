package com.supportops.api.modules.auth.service.email;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Override
    public String provider() {
        return "smtp";
    }

    @Override
    public void send(EmailMessage emailMessage) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(emailMessage.to());
            helper.setFrom(emailMessage.from(), emailMessage.fromName());
            helper.setSubject(emailMessage.subject());
            helper.setText(emailMessage.textBody(), false);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to send email via SMTP", ex);
        }
    }
}
