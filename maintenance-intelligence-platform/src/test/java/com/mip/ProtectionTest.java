package com.mip;

import com.mip.common.timelimit.TimeLimitedExecutor;
import com.mip.exception.OperationTimeoutException;
import com.mip.plant.entity.Plant;
import com.mip.security.ratelimit.RateLimiterRegistry;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The rate limiter (per-tier budgets, 429 + Retry-After) and the time limiter. */
@TestPropertySource(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.auth-limit=3",
        "app.rate-limit.auth-window-seconds=60",
        "app.rate-limit.general-limit=8",
        "app.rate-limit.general-window-seconds=60"
})
class ProtectionTest extends IntegrationTestBase {

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;
    @Autowired
    private TimeLimitedExecutor timeLimitedExecutor;

    private Plant plant;
    private User engineer;

    @BeforeEach
    void setUp() {
        rateLimiterRegistry.clear();
        plant = newPlant();
        engineer = newUser(RoleName.ENGINEER, plant);
    }

    @Test
    void credentialEndpointsGetAStrictPerIpBudget() throws Exception {
        String badLogin = "{\"email\":\"" + engineer.getEmail() + "\",\"password\":\"WrongPass1\"}";
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(badLogin))
                    .andExpect(status().isUnauthorized());
        }
        // the fourth attempt in the window is throttled - even with correct credentials
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(badLogin))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + engineer.getEmail() + "\",\"password\":\""
                                + PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void generalBudgetIsPerUserSoOneClientCannotStarveOthers() throws Exception {
        String token = login(engineer);
        for (int i = 0; i < 8; i++) {
            mockMvc.perform(get("/api/plants").header("Authorization", bearer(token)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/plants").header("Authorization", bearer(token)))
                .andExpect(status().isTooManyRequests());

        // a different user has an untouched budget
        User colleague = newUser(RoleName.ENGINEER, plant);
        mockMvc.perform(get("/api/plants").header("Authorization", bearer(login(colleague))))
                .andExpect(status().isOk());
    }

    @Test
    void staticPagesAreNotRateLimited() throws Exception {
        for (int i = 0; i < 12; i++) {
            mockMvc.perform(get("/reset-password.html")).andExpect(status().isOk());
        }
    }

    @Test
    void timeLimiterAbortsSlowOperationsAndPassesFastOnes() {
        String result = timeLimitedExecutor.call(() -> "done", Duration.ofSeconds(2), "fast op");
        assertThat(result).isEqualTo("done");

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> timeLimitedExecutor.call(() -> {
            Thread.sleep(10_000);
            return "never";
        }, Duration.ofMillis(300), "slow op"))
                .isInstanceOf(OperationTimeoutException.class)
                .hasMessageContaining("slow op");
        // the caller was released at the budget, not after the task's full duration
        assertThat(System.currentTimeMillis() - start).isLessThan(5_000);
    }
}
