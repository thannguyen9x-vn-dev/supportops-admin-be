package com.supportops.api.modules.auth.service.email;

public record EmailMessage(
    String to,
    String from,
    String fromName,
    String subject,
    String textBody
) {
}
