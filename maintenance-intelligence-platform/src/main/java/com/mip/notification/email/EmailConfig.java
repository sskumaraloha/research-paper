package com.mip.notification.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * SMTP delivery when spring.mail.host is configured; otherwise emails are logged to
 * the console so the reset flow stays fully usable in development.
 */
@Configuration
@Slf4j
public class EmailConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.mail.host")
    public EmailService smtpEmailService(JavaMailSender mailSender,
                                         @Value("${app.mail.from:no-reply@mip.local}") String from) {
        return (to, subject, body) -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email '{}' sent to {}", subject, to);
        };
    }

    @Bean
    @ConditionalOnMissingBean(EmailService.class)
    public EmailService loggingEmailService() {
        return (to, subject, body) ->
                log.info("EMAIL (console delivery)\nTo: {}\nSubject: {}\n{}", to, subject, body);
    }
}
