package com.mip;

import com.fasterxml.jackson.databind.JsonNode;
import com.mip.plant.entity.Plant;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowTest extends IntegrationTestBase {

    private User engineer;
    private Plant plant;

    @BeforeEach
    void setUp() {
        plant = newPlant();
        engineer = newUser(RoleName.ENGINEER, plant);
    }

    @Test
    void loginReturnsTokensAndProfile() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + engineer.getEmail() + "\",\"password\":\""
                                + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("ENGINEER"))
                .andReturn();
        assertThat(json(result).get("user").get("plantIds").get(0).asLong())
                .isEqualTo(plant.getId());
    }

    @Test
    void wrongPasswordIsRejectedWithoutDetail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + engineer.getEmail()
                                + "\",\"password\":\"WrongPass1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void refreshRotatesAndOldTokenDies() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + engineer.getEmail() + "\",\"password\":\""
                                + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn();
        String refreshToken = json(loginResult).get("refreshToken").asText();
        String refreshBody = "{\"refreshToken\":\"" + refreshToken + "\"}";

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // the presented token was consumed by rotation
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + engineer.getEmail() + "\",\"password\":\""
                                + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode tokens = json(loginResult);
        String body = "{\"refreshToken\":\"" + tokens.get("refreshToken").asText() + "\"}";

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer(tokens.get("accessToken").asText()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsRequireAuthenticationAndRoles() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/plants")).andExpect(status().isUnauthorized());

        String token = login(engineer);
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(engineer.getEmail()));

        // user listing is admin-only
        mockMvc.perform(get("/api/users").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String token = login(engineer);
        String tampered = token.substring(0, token.length() - 3) + "abc";
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }
}
