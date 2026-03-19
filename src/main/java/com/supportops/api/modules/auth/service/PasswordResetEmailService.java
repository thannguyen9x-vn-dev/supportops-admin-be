package com.supportops.api.modules.auth.service;

import com.supportops.api.config.AppMailProperties;
import com.supportops.api.modules.auth.service.email.EmailMessage;
import com.supportops.api.modules.auth.service.email.EmailSender;
import com.supportops.api.modules.user.entity.User;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {

    private final List<EmailSender> emailSenders;
    private final AppMailProperties mailProperties;

    public boolean isEnabled() {
        return mailProperties.isEnabled();
    }

    public void sendResetPasswordEmail(User user, String rawToken) {
        String locale = normalizeLocale(user.getLocale());
        String resetUrl = buildResetUrl(locale, rawToken);

        String subject = "Reset your SupportOps password";
        String text = """
            Hello %s,

            We received a request to reset your password.

            Reset your password using this link:
            %s

            This link will expire in 15 minutes and can only be used once.
            If you did not request a password reset, you can ignore this email.

            Best regards,
            %s
            """.formatted(
            safeName(user.getFirstName()),
            resetUrl,
            mailProperties.getSupportName()
        );

        EmailSender emailSender = resolveSender();
        emailSender.send(new EmailMessage(
            user.getEmail(),
            mailProperties.getFrom(),
            mailProperties.getSupportName(),
            subject,
            text
        ));
    }

    private String buildResetUrl(String locale, String rawToken) {
        String encodedToken = urlEncode(rawToken);
        String template = mailProperties.getPasswordResetUrlTemplate();
        return template
            .replace("{locale}", locale)
            .replace("{token}", encodedToken);
    }

    private String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "en";
        }
        return locale.trim().toLowerCase(Locale.ROOT);
    }

    private String safeName(String firstName) {
        if (firstName == null || firstName.isBlank()) {
            return "there";
        }
        return firstName.trim();
    }

    private String urlEncode(String input) {
        try {
            return URLEncoder.encode(input, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            return input;
        }
    }

    private EmailSender resolveSender() {
        String provider = mailProperties.getProvider() == null
            ? "smtp"
            : mailProperties.getProvider().trim().toLowerCase(Locale.ROOT);

        return emailSenders.stream()
            .filter(sender -> sender.provider().equalsIgnoreCase(provider))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No email sender configured for provider: " + provider));
    }
}
