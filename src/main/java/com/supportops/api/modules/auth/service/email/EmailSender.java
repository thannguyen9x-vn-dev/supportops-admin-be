package com.supportops.api.modules.auth.service.email;

public interface EmailSender {

    String provider();

    void send(EmailMessage emailMessage);
}
