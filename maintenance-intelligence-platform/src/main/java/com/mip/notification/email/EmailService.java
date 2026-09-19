package com.mip.notification.email;

/** Outbound email abstraction: SMTP in real deployments, console logging in dev/test. */
public interface EmailService {

    void send(String to, String subject, String body);
}
