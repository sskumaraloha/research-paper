package com.mip;

import com.fasterxml.jackson.databind.JsonNode;
import com.mip.dictionary.entity.FailureCategory;
import com.mip.machine.entity.Machine;
import com.mip.notification.repository.NotificationRepository;
import com.mip.plant.entity.Plant;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.entity.RecordSource;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EntryAgentTest extends IntegrationTestBase {

    @Autowired
    private NotificationRepository notificationRepository;

    private Plant plant;
    private Machine lathe;
    private Machine conveyor;
    private User engineer;
    private User colleague;
    private com.mip.dictionary.entity.FailureMode belt;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        plant = newPlant();
        lathe = newMachine(plant, null, "CNC-01", "CNC Lathe 01");
        conveyor = newMachine(plant, null, "CONV-A", "Conveyor A");
        engineer = newUser(RoleName.ENGINEER, plant);
        colleague = newUser(RoleName.ENGINEER, plant);
        ensureFailureMode("FM-BRG", "Bearing Failure", FailureCategory.MECHANICAL,
                "bearing,bearing seized");
        belt = ensureFailureMode("FM-BELT", "Belt Breakage", FailureCategory.MECHANICAL,
                "belt,belt snapped");
        token = login(engineer);
    }

    @Test
    void oneShotMessageIsFullyExtractedAndConfirmed() throws Exception {
        MvcResult started = mockMvc.perform(post("/api/entry/conversations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantId\":" + plant.getId() + ",\"message\":"
                                + "\"CNC-01 bearing seized this morning, machine was down 2 hours\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AWAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.draft.machineId").value(lathe.getId()))
                .andExpect(jsonPath("$.draft.downtimeMinutes").value(120))
                .andExpect(jsonPath("$.draft.recordDate").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.draft.failureMode").value("Bearing Failure"))
                .andReturn();
        long conversationId = json(started).get("id").asLong();

        MvcResult confirmed = mockMvc.perform(post("/api/entry/conversations/" + conversationId
                        + "/confirm").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.resultingRecordId").isNumber())
                .andReturn();
        long recordId = json(confirmed).get("resultingRecordId").asLong();

        MaintenanceRecord record = recordRepository.findById(recordId).orElseThrow();
        assertThat(record.getSource()).isEqualTo(RecordSource.ENTRY_AGENT);
        assertThat(record.getMachine().getId()).isEqualTo(lathe.getId());
        assertThat(record.getDowntimeMinutes()).isEqualTo(120);

        // the colleague was notified, the author was not
        assertThat(notificationRepository.countByUserIdAndReadFlagFalse(colleague.getId()))
                .isGreaterThan(0);
        assertThat(notificationRepository.countByUserIdAndReadFlagFalse(engineer.getId()))
                .isZero();

        // a closed conversation refuses more messages
        mockMvc.perform(post("/api/entry/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"one more thing\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void agentCollectsMissingFieldsAcrossTurns() throws Exception {
        MvcResult started = mockMvc.perform(post("/api/entry/conversations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantId\":" + plant.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COLLECTING"))
                .andReturn();
        long conversationId = json(started).get("id").asLong();

        // vague first report: no confident machine, no duration
        mockMvc.perform(post("/api/entry/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"belt snapped on the packing side yesterday\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTING"))
                .andExpect(jsonPath("$.draft.recordDate").value(
                        LocalDate.now().minusDays(1).toString()));

        mockMvc.perform(post("/api/entry/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"it was CONV-A, down for 45 min\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.draft.machineId").value(conveyor.getId()))
                .andExpect(jsonPath("$.draft.downtimeMinutes").value(45));

        // the user corrects the duration before confirming
        mockMvc.perform(post("/api/entry/conversations/" + conversationId + "/request-edit")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTING"));
        mockMvc.perform(post("/api/entry/conversations/" + conversationId + "/messages")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"actually it was down 90 minutes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.draft.downtimeMinutes").value(90));

        MvcResult confirmed = mockMvc.perform(post("/api/entry/conversations/" + conversationId
                        + "/confirm").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode response = json(confirmed);
        MaintenanceRecord record = recordRepository
                .findById(response.get("resultingRecordId").asLong()).orElseThrow();
        assertThat(record.getDowntimeMinutes()).isEqualTo(90);
        // proxy-safe: only the id is read from the lazy failure-mode reference
        assertThat(record.getFailureMode().getId()).isEqualTo(belt.getId());
    }

    @Test
    void conversationsAreOwnerScopedAndViewerForbidden() throws Exception {
        MvcResult started = mockMvc.perform(post("/api/entry/conversations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantId\":" + plant.getId() + "}"))
                .andExpect(status().isCreated()).andReturn();
        long conversationId = json(started).get("id").asLong();

        // another engineer cannot read someone else's conversation
        mockMvc.perform(get("/api/entry/conversations/" + conversationId)
                        .header("Authorization", bearer(login(colleague))))
                .andExpect(status().isNotFound());

        User viewer = newUser(RoleName.VIEWER, plant);
        mockMvc.perform(post("/api/entry/conversations")
                        .header("Authorization", bearer(login(viewer)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantId\":" + plant.getId() + "}"))
                .andExpect(status().isForbidden());
    }
}
