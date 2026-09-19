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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecordFlowTest extends IntegrationTestBase {

    private Plant plant;
    private ProductionLine line;
    private Machine machine;
    private User engineer;
    private FailureMode bearing;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        plant = newPlant();
        line = newLine(plant);
        machine = newMachine(plant, line, "RC-" + nextId(), "Record Machine");
        engineer = newUser(RoleName.ENGINEER, plant);
        bearing = ensureFailureMode("FM-BRG", "Bearing Failure", FailureCategory.MECHANICAL,
                "bearing,bearing seized");
        token = login(engineer);
    }

    @Test
    void createResolveRejectAndTimeline() throws Exception {
        String body = """
                {"plantId": %d, "machineId": %d, "recordDate": "%s", "downtimeMinutes": 75,
                 "description": "Bearing seized on the output shaft", "actionTaken": "Replaced it",
                 "technician": "N. Rao", "partNames": ["Ball Bearing 6205"]}
                """.formatted(plant.getId(), machine.getId(), LocalDate.now().minusDays(1));

        MvcResult created = mockMvc.perform(post("/api/records")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                // the failure mode was resolved from the free-text description
                .andExpect(jsonPath("$.failureMode").value("Bearing Failure"))
                .andExpect(jsonPath("$.spareParts.length()").value(1))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andReturn();
        long recordId = json(created).get("id").asLong();

        // timeline and filtered listing both see it
        mockMvc.perform(get("/api/machines/" + machine.getId() + "/timeline")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(recordId));
        mockMvc.perform(get("/api/records")
                        .param("plantId", plant.getId().toString())
                        .param("failureModeId", bearing.getId().toString())
                        .param("text", "output shaft")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // filter options reflect what is actually in use
        mockMvc.perform(get("/api/records/filter-options")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failureModes[0].name").value("Bearing Failure"));

        // rejection soft-deletes: gone from listings, second reject refused
        mockMvc.perform(post("/api/records/" + recordId + "/reject")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Duplicate entry\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        mockMvc.perform(get("/api/records")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(post("/api/records/" + recordId + "/reject")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"again\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void validationRulesRejectBadInput() throws Exception {
        // future date and blank description are both refused with field errors
        String body = """
                {"plantId": %d, "machineId": %d, "recordDate": "%s",
                 "downtimeMinutes": -5, "description": ""}
                """.formatted(plant.getId(), machine.getId(), LocalDate.now().plusDays(2));
        mockMvc.perform(post("/api/records")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.recordDate").exists())
                .andExpect(jsonPath("$.fieldErrors.description").exists())
                .andExpect(jsonPath("$.fieldErrors.downtimeMinutes").exists());

        // a machine from another plant is unreachable
        Plant foreign = newPlant();
        Machine foreignMachine = newMachine(foreign, null, "FX-" + nextId(), "Foreign");
        String crossPlant = """
                {"plantId": %d, "machineId": %d, "recordDate": "%s",
                 "downtimeMinutes": 10, "description": "cross-plant probe"}
                """.formatted(plant.getId(), foreignMachine.getId(), LocalDate.now());
        mockMvc.perform(post("/api/records")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(crossPlant))
                .andExpect(status().isNotFound());
    }
}
