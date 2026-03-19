package com.supportops.api.modules.auth.service.email;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "ses")
public class SesEmailSender implements EmailSender {

    private final SesClient sesClient;

    @Override
    public String provider() {
        return "ses";
    }

    @Override
    public void send(EmailMessage emailMessage) {
        String source = buildSource(emailMessage.fromName(), emailMessage.from());

        SendEmailRequest request = SendEmailRequest.builder()
            .source(source)
            .destination(Destination.builder().toAddresses(emailMessage.to()).build())
            .message(
                Message.builder()
                    .subject(Content.builder().data(emailMessage.subject()).charset("UTF-8").build())
                    .body(
                        Body.builder()
                            .text(Content.builder().data(emailMessage.textBody()).charset("UTF-8").build())
                            .build()
                    )
                    .build()
            )
            .build();

        sesClient.sendEmail(request);
    }

    private String buildSource(String fromName, String from) {
        if (fromName == null || fromName.isBlank()) {
            return from;
        }
        return "%s <%s>".formatted(fromName.trim(), from);
    }
}
