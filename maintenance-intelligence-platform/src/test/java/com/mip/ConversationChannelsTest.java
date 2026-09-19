package com.mip;

import com.fasterxml.jackson.databind.JsonNode;
import com.mip.dictionary.entity.FailureCategory;
import com.mip.machine.entity.Machine;
import com.mip.plant.entity.Plant;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.entity.RecordSource;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Persisted assistant conversations and the WhatsApp inbound channel. */
class ConversationChannelsTest extends IntegrationTestBase {

    private static final String WEBHOOK_TOKEN = "test-webhook-token";

    private Plant plant;
    private Machine lathe;
    private User engineer;
    private String token;
    private String phone;

    @BeforeEach
    void setUp() throws Exception {
        plant = newPlant();
        lathe = newMachine(plant, null, "CNC-01", "CNC Lathe 01");
        engineer = newUser(RoleName.ENGINEER, plant);
        phone = "9188%08d".formatted(nextId());
        engineer.setPhoneNumber(phone);
        userRepository.save(engineer);
        ensureFailureMode("FM-BRG", "Bearing Failure", FailureCategory.MECHANICAL,
                "bearing,bearing seized");
        newRecord(plant, lathe, null, LocalDate.now().minusDays(3), 60, engineer);
        token = login(engineer);
    }

    @Test
    void assistantConversationsPersistAndContinue() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/assistant/ask")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantId\":" + plant.getId()
                                + ",\"question\":\"Which machine has the highest downtime?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").isNumber())
                .andReturn();
        long conversationId = json(first).get("conversationId").asLong();

        // a follow-up in the same conversation keeps the same id
        mockMvc.perform(post("/api/assistant/ask")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantId\":" + plant.getId() + ",\"conversationId\":"
                                + conversationId + ",\"question\":\"Show plant KPIs\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId));

        // history holds both exchanges in order, titled by the first question
        mockMvc.perform(get("/api/assistant/conversations/" + conversationId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Which machine has the highest downtime?"))
                .andExpect(jsonPath("$.messages.length()").value(4))
                .andExpect(jsonPath("$.messages[0].sender").value("USER"))
                .andExpect(jsonPath("$.messages[3].intent").value("PLANT_KPIS"));

        mockMvc.perform(get("/api/assistant/conversations")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conversationId));

        // another user's conversation is unreachable
        User other = newUser(RoleName.ENGINEER, plant);
        mockMvc.perform(get("/api/assistant/conversations/" + conversationId)
                        .header("Authorization", bearer(login(other))))
                .andExpect(status().isNotFound());
    }

    @Test
    void whatsAppInboundDrivesTheEntryAgentEndToEnd() throws Exception {
        // one message with everything → draft awaiting confirmation
        MvcResult first = mockMvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Webhook-Token", WEBHOOK_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"" + phone + "\",\"text\":"
                                + "\"CNC-01 bearing seized today, down 2 hours\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_CONFIRMATION"))
                .andReturn();
        JsonNode firstReply = json(first);
        assertThat(firstReply.get("reply").asText()).contains("CNC Lathe 01");

        // "confirm" over WhatsApp saves the record
        MvcResult confirmed = mockMvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Webhook-Token", WEBHOOK_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"" + phone + "\",\"text\":\"confirm\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn();
        assertThat(json(confirmed).get("conversationId").asLong())
                .isEqualTo(firstReply.get("conversationId").asLong());

        MaintenanceRecord record = recordRepository.findAll().stream()
                .filter(r -> r.getSource() == RecordSource.ENTRY_AGENT)
                .reduce((a, b) -> b).orElseThrow();
        assertThat(record.getDowntimeMinutes()).isEqualTo(120);
        assertThat(record.getMachine().getId()).isEqualTo(lathe.getId());
    }

    @Test
    void webhookRejectsBadTokensAndUnknownSenders() throws Exception {
        String body = "{\"from\":\"" + phone + "\",\"text\":\"pump tripped\"}";

        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Webhook-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Webhook-Token", WEBHOOK_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"910000000000\",\"text\":\"pump tripped\"}"))
                .andExpect(status().isUnauthorized());

        // a read-only account cannot log records over WhatsApp either
        User viewer = newUser(RoleName.VIEWER, plant);
        viewer.setPhoneNumber("9177%08d".formatted(nextId()));
        userRepository.save(viewer);
        mockMvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Webhook-Token", WEBHOOK_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"" + viewer.getPhoneNumber()
                                + "\",\"text\":\"pump tripped\"}"))
                .andExpect(status().isForbidden());
    }
}
