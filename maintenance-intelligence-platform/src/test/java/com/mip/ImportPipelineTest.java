package com.mip;

import com.fasterxml.jackson.databind.JsonNode;
import com.mip.dictionary.entity.FailureCategory;
import com.mip.machine.entity.Machine;
import com.mip.machine.repository.AliasSuggestionRepository;
import com.mip.notification.repository.NotificationRepository;
import com.mip.plant.entity.Plant;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImportPipelineTest extends IntegrationTestBase {

    @Autowired
    private AliasSuggestionRepository aliasSuggestionRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private Plant plant;
    private User engineer;
    private User admin;
    private Machine cnc;
    private Machine mixer;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        plant = newPlant();
        engineer = newUser(RoleName.ENGINEER, plant);
        admin = newUser(RoleName.ADMIN);
        cnc = newMachine(plant, null, "CNC-77", "CNC Lathe 77");
        mixer = newMachine(plant, null, "MYS-01", "Mystery Mixer");
        ensureFailureMode("FM-BRG", "Bearing Failure", FailureCategory.MECHANICAL,
                "bearing,bearing seized");
        ensureFailureMode("FM-BELT", "Belt Breakage", FailureCategory.MECHANICAL,
                "belt,belt snapped");
        ensureFailureMode("FM-MOT", "Motor Burnout", FailureCategory.ELECTRICAL,
                "motor,motor tripped");
        token = login(engineer);
    }

    @Test
    void pipelineRoutesRowsAndValidationLifecycleWorks() throws Exception {
        String csv = """
                Machine,Date,Downtime (min),Problem Description,Action Taken,Technician,Parts Used
                CNC-77,2026-09-01,120,Bearing seized on spindle,Replaced bearing,R. K,Ball Bearing 6205
                Mystery Grinder X,2026-09-02,60,Belt snapped near drive,Re-fitted,S. P,
                ,,45,,,J. Doe,
                CNC-77,,90,Motor tripped during startup,Reset breaker,A. M,
                """;

        MvcResult uploadResult = mockMvc.perform(multipart("/api/imports/upload")
                        .file(csvFile(csv))
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRows").value(4))
                .andExpect(jsonPath("$.autoImportedCount").value(1))
                .andExpect(jsonPath("$.needsValidationCount").value(2))
                .andExpect(jsonPath("$.invalidCount").value(1))
                .andExpect(jsonPath("$.rejectedCount").value(0))
                .andReturn();
        long jobId = json(uploadResult).get("jobId").asLong();

        // the confident row became a real record with parts and provenance
        MvcResult listResult = mockMvc.perform(get("/api/records")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].source").value("IMPORT"))
                .andExpect(jsonPath("$.content[0].machineId").value(cnc.getId()))
                .andExpect(jsonPath("$.content[0].failureMode").value("Bearing Failure"))
                .andReturn();
        long importedRecordId = json(listResult).get("content").get(0).get("id").asLong();
        assertThat(json(listResult).get("content").get(0).get("confidence").asDouble())
                .isGreaterThanOrEqualTo(0.85);
        mockMvc.perform(get("/api/records/" + importedRecordId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spareParts.length()").value(1))
                .andExpect(jsonPath("$.sourceDocumentId").isNumber());

        // job detail exposes the six pipeline steps
        mockMvc.perform(get("/api/imports/" + jobId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED_WITH_ERRORS"))
                .andExpect(jsonPath("$.steps.length()").value(6));

        // an unresolved machine name accumulated as an alias suggestion
        var suggestion = aliasSuggestionRepository
                .findByPlantIdAndRawText(plant.getId(), "mystery grinder x").orElseThrow();

        // validation queue holds both flagged rows
        MvcResult queueResult = mockMvc.perform(get("/api/validation/queue")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCount").value(2))
                .andReturn();
        JsonNode items = json(queueResult).get("items").get("content");

        long unresolvedItemId = 0;
        long missingDateItemId = 0;
        for (JsonNode item : items) {
            if (item.get("machineId").isNull()) {
                unresolvedItemId = item.get("id").asLong();
            } else {
                missingDateItemId = item.get("id").asLong();
            }
        }

        // plain approve is refused while data is missing
        mockMvc.perform(post("/api/validation/" + missingDateItemId + "/approve")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isUnprocessableEntity());

        // edit-approve supplies the missing date and imports the row
        mockMvc.perform(post("/api/validation/" + missingDateItemId + "/edit-approve")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordDate\":\"2026-09-05\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.recordId").isNumber());

        // mapping the alias suggestion re-resolves the unresolved queue item
        mockMvc.perform(post("/api/validation/alias-suggestions/" + suggestion.getId() + "/map")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"machineId\":" + mixer.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revalidatedItemCount").value(1));

        mockMvc.perform(post("/api/validation/" + unresolvedItemId + "/approve")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/validation/pending-count")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        // the learned alias now auto-imports the once-unresolvable name
        String secondCsv = """
                Machine,Date,Downtime (min),Problem Description,Technician
                Mystery Grinder X,2026-09-10,30,Bearing noise on main shaft,S. P
                """;
        mockMvc.perform(multipart("/api/imports/upload")
                        .file(csvFile(secondCsv))
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autoImportedCount").value(1))
                .andExpect(jsonPath("$.needsValidationCount").value(0));

        // validation-pending notification reached the admin
        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(admin.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 10)).getContent())
                .anyMatch(n -> n.getType().name().equals("VALIDATION_PENDING"));
    }

    @Test
    void identicalFileIsRejectedAndViewersCannotUpload() throws Exception {
        String csv = """
                Machine,Date,Downtime (min),Problem Description
                CNC-77,2026-09-03,15,Bearing check
                """;
        mockMvc.perform(multipart("/api/imports/upload")
                        .file(csvFile(csv))
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated());
        mockMvc.perform(multipart("/api/imports/upload")
                        .file(csvFile(csv))
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isConflict());

        User viewer = newUser(RoleName.VIEWER, plant);
        mockMvc.perform(multipart("/api/imports/upload")
                        .file(csvFile("Machine,Date\nCNC-77,2026-09-04"))
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(login(viewer))))
                .andExpect(status().isForbidden());
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "maintenance-log-" + nextId() + ".csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
