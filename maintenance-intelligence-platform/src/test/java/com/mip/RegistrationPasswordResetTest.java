package com.mip;

import com.fasterxml.jackson.databind.JsonNode;
import com.mip.notification.email.EmailService;
import com.mip.plant.entity.Plant;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistrationPasswordResetTest extends IntegrationTestBase {

    record SentEmail(String to, String subject, String body) {
    }

    static final List<SentEmail> SENT = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class MailCapture {
        @Bean
        @Primary
        EmailService capturingEmailService() {
            return (to, subject, body) -> SENT.add(new SentEmail(to, subject, body));
        }
    }

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("token=([A-Za-z0-9_-]+)");

    private Plant plant;
    private User engineer;

    @BeforeEach
    void setUp() {
        SENT.clear();
        plant = newPlant();
        engineer = newUser(RoleName.ENGINEER, plant);
    }

    @Test
    void registrationCreatesViewerAndLogsIn() throws Exception {
        String email = "signup" + nextId() + "@test.local";
        String body = """
                {"fullName": "Self Signup", "email": "%s", "password": "%s"}
                """.formatted(email, PASSWORD);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("VIEWER"))
                .andExpect(jsonPath("$.user.email").value(email));

        // the fresh account can log in on its own
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());

        // duplicate email is a conflict, weak password a validation error
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"X\",\"email\":\"weak" + nextId()
                                + "@test.local\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void forgotPasswordEmailsALinkThatResetsThePassword() throws Exception {
        // hold a refresh token from before the reset to prove revocation
        MvcResult preReset = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + engineer.getEmail() + "\",\"password\":\""
                                + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn();
        String oldRefreshToken = json(preReset).get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + engineer.getEmail() + "\"}"))
                .andExpect(status().isOk());

        assertThat(SENT).hasSize(1);
        SentEmail email = SENT.get(0);
        assertThat(email.to()).isEqualTo(engineer.getEmail());
        assertThat(email.body()).contains("/reset-password.html?token=");
        Matcher matcher = TOKEN_IN_LINK.matcher(email.body());
        assertThat(matcher.find()).isTrue();
        String token = matcher.group(1);

        // the page the link opens is publicly reachable, and the token validates
        mockMvc.perform(get("/reset-password.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Set a new password")));
        mockMvc.perform(get("/api/auth/reset-password/validate").param("token", token))
                .andExpect(status().isOk());

        String newPassword = "Brand-New-Pass1";
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\""
                                + newPassword + "\"}"))
                .andExpect(status().isOk());

        // old password dead, new password works, pre-reset refresh token revoked
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + engineer.getEmail() + "\",\"password\":\""
                                + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + engineer.getEmail() + "\",\"password\":\""
                                + newPassword + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        // the link is single-use
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\"AnotherPass1\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/reset-password/validate").param("token", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPasswordNeverLeaksAccountsAndRateLimits() throws Exception {
        // an unknown email gets the same friendly answer and no email is sent
        MvcResult unknown = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody" + nextId() + "@test.local\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(SENT).isEmpty();
        JsonNode reply = json(unknown);

        // a known email gets the identical message
        MvcResult known = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + engineer.getEmail() + "\"}"))
                .andExpect(status().isOk()).andReturn();
        assertThat(json(known).get("message").asText()).isEqualTo(reply.get("message").asText());
        assertThat(SENT).hasSize(1);

        // per-account cap: two more succeed, the fourth in the hour is rejected
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + engineer.getEmail() + "\"}"))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + engineer.getEmail() + "\"}"))
                .andExpect(status().isTooManyRequests());
        assertThat(SENT).hasSize(3);
    }
}
