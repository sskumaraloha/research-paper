package com.mip;

import com.mip.dictionary.entity.FailureCategory;
import com.mip.dictionary.entity.FailureMode;
import com.mip.machine.entity.Machine;
import com.mip.plant.entity.Plant;
import com.mip.plant.entity.ProductionLine;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsInsightTest extends IntegrationTestBase {

    private Plant plant;
    private Machine chronic;
    private Machine healthy;
    private User engineer;
    private FailureMode bearing;
    private FailureMode motor;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        plant = newPlant();
        ProductionLine line = newLine(plant);
        chronic = newMachine(plant, line, "CH-" + nextId(), "Chronic Machine");
        healthy = newMachine(plant, line, "HL-" + nextId(), "Healthy Machine");
        engineer = newUser(RoleName.ENGINEER, plant);
        bearing = ensureFailureMode("FM-BRG", "Bearing Failure", FailureCategory.MECHANICAL,
                "bearing");
        motor = ensureFailureMode("FM-MOT", "Motor Burnout", FailureCategory.ELECTRICAL, "motor");

        // four recent bearing failures on the chronic machine, one motor failure elsewhere
        LocalDate today = LocalDate.now();
        newRecord(plant, chronic, bearing, today.minusDays(2), 100, engineer);
        newRecord(plant, chronic, bearing, today.minusDays(8), 100, engineer);
        newRecord(plant, chronic, bearing, today.minusDays(15), 100, engineer);
        newRecord(plant, chronic, bearing, today.minusDays(21), 100, engineer);
        newRecord(plant, healthy, motor, today.minusDays(5), 50, engineer);
        token = login(engineer);
    }

    @Test
    void paretoTopMachinesAndKpisAddUp() throws Exception {
        mockMvc.perform(get("/api/analytics/pareto")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].failureMode").value("Bearing Failure"))
                .andExpect(jsonPath("$[0].downtimeMinutes").value(400))
                .andExpect(jsonPath("$[0].downtimeSharePct").value(88.9))
                .andExpect(jsonPath("$[1].cumulativeSharePct").value(100.0));

        mockMvc.perform(get("/api/analytics/top-downtime-machines")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].machineId").value(chronic.getId()))
                .andExpect(jsonPath("$[0].downtimeMinutes").value(400));

        mockMvc.perform(get("/api/analytics/line-downtime-share")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].downtimeMinutes").value(450))
                .andExpect(jsonPath("$[0].sharePct").value(100.0));

        mockMvc.perform(get("/api/dashboard/kpis")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis[?(@.key=='downtimeMinutes')].value").value(450.0))
                .andExpect(jsonPath("$.kpis[?(@.key=='maintenanceEvents')].value").value(5.0));

        mockMvc.perform(get("/api/machines/" + chronic.getId() + "/stats")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordCount").value(4))
                .andExpect(jsonPath("$.totalDowntimeMinutes").value(400))
                // 19 days between first and last failure over 3 intervals
                .andExpect(jsonPath("$.mtbfDays").value(6.3));
    }

    @Test
    void detectorsFindTheChronicPatternsAndBadgesCount() throws Exception {
        MvcResult recompute = mockMvc.perform(post("/api/insights/recompute")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        String body = recompute.getResponse().getContentAsString();
        assertThat(body).contains("REPEATED_FAILURES");
        assertThat(body).contains("CHRONIC_TOP_MACHINE");
        assertThat(body).contains("DOMINANT_FAILURE_MODE");

        mockMvc.perform(get("/api/machines/" + chronic.getId() + "/insights")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='REPEATED_FAILURES')].metricValue").value(4.0))
                .andExpect(jsonPath("$[?(@.type=='REPEATED_FAILURES')].evidence.length()").value(4));

        mockMvc.perform(get("/api/notifications/badge-counts")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingValidation").value(0))
                .andExpect(jsonPath("$.insights").isNumber());
    }
}
