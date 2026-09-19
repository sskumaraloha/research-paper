package com.mip;

import com.mip.machine.entity.AliasSuggestion;
import com.mip.machine.entity.Machine;
import com.mip.machine.repository.AliasSuggestionRepository;
import com.mip.plant.entity.Plant;
import com.mip.plant.entity.ProductionLine;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminMasterDataTest extends IntegrationTestBase {

    @Autowired
    private AliasSuggestionRepository aliasSuggestionRepository;

    private Plant plant;
    private User admin;
    private User engineer;
    private String adminToken;
    private String engineerToken;

    @BeforeEach
    void setUp() throws Exception {
        plant = newPlant();
        admin = newUser(RoleName.ADMIN);
        engineer = newUser(RoleName.ENGINEER, plant);
        adminToken = login(admin);
        engineerToken = login(engineer);
    }

    @Test
    void adminCreatesUpdatesAndDeactivatesUsers() throws Exception {
        String email = "new" + nextId() + "@test.local";
        String createBody = """
                {"fullName": "New Engineer", "email": "%s", "password": "%s",
                 "role": "ENGINEER", "phoneNumber": "917000000%03d", "plantIds": [%d]}
                """.formatted(email, PASSWORD, nextId() % 1000, plant.getId());

        MvcResult created = mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ENGINEER"))
                .andExpect(jsonPath("$.plantIds[0]").value(plant.getId()))
                .andReturn();
        long newUserId = json(created).get("id").asLong();

        // duplicate email is a conflict
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isConflict());

        // the new account can actually log in
        User newUser = userRepository.findById(newUserId).orElseThrow();
        String newUserToken = login(newUser);

        // role change takes effect
        mockMvc.perform(put("/api/users/" + newUserId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VIEWER"));

        // self-deactivation is blocked, deactivating others works and kills the login
        mockMvc.perform(post("/api/users/" + admin.getId() + "/deactivate")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/api/users/" + newUserId + "/deactivate")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
        // the deactivated user's existing access token stops working immediately
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/auth/me").header("Authorization", bearer(newUserToken)))
                .andExpect(status().isUnauthorized());

        // engineers cannot manage users at all
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(engineerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUpdatesPlantSettingsWithGuardrails() throws Exception {
        mockMvc.perform(put("/api/plants/" + plant.getId() + "/settings")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoApproveThreshold\":0.9,\"lowConfidenceThreshold\":0.5,"
                                + "\"downtimeAlertMinutes\":180}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoApproveThreshold").value(0.9))
                .andExpect(jsonPath("$.downtimeAlertMinutes").value(180));

        // low threshold must sit below the auto-approve threshold
        mockMvc.perform(put("/api/plants/" + plant.getId() + "/settings")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoApproveThreshold\":0.5,\"lowConfidenceThreshold\":0.6,"
                                + "\"downtimeAlertMinutes\":180}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.lowConfidenceThreshold").exists());

        // settings are admin-only
        mockMvc.perform(put("/api/plants/" + plant.getId() + "/settings")
                        .header("Authorization", bearer(engineerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoApproveThreshold\":0.9,\"lowConfidenceThreshold\":0.5,"
                                + "\"downtimeAlertMinutes\":180}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void machinesAreCreatedAndUpdatedWithPlantConsistency() throws Exception {
        ProductionLine line = newLine(plant);
        String code = "NEW-" + nextId();
        String createBody = """
                {"plantId": %d, "lineId": %d, "code": "%s", "name": "New Grinder",
                 "manufacturer": "ACME", "criticality": "HIGH", "commissionedOn": "2024-01-15"}
                """.formatted(plant.getId(), line.getId(), code);

        MvcResult created = mockMvc.perform(post("/api/machines")
                        .header("Authorization", bearer(engineerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.lineName").value(line.getName()))
                .andExpect(jsonPath("$.criticality").value("HIGH"))
                .andReturn();
        long machineId = json(created).get("id").asLong();

        // duplicate code within the plant is a conflict
        mockMvc.perform(post("/api/machines")
                        .header("Authorization", bearer(engineerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isConflict());

        // a line from another plant is unreachable
        Plant foreign = newPlant();
        ProductionLine foreignLine = newLine(foreign);
        mockMvc.perform(put("/api/machines/" + machineId)
                        .header("Authorization", bearer(engineerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lineId\":" + foreignLine.getId() + "}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/machines/" + machineId)
                        .header("Authorization", bearer(engineerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed Grinder\",\"criticality\":\"CRITICAL\","
                                + "\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Grinder"))
                .andExpect(jsonPath("$.criticality").value("CRITICAL"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void aliasSuggestionsCanBeDismissed() throws Exception {
        Machine machine = newMachine(plant, null, "AD-" + nextId(), "Alias Machine");
        AliasSuggestion suggestion = aliasSuggestionRepository.save(
                new AliasSuggestion(plant, "weird import name " + nextId(), machine, 0.5));

        mockMvc.perform(post("/api/validation/alias-suggestions/" + suggestion.getId() + "/dismiss")
                        .header("Authorization", bearer(engineerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));

        // dismissing twice is an invalid transition
        mockMvc.perform(post("/api/validation/alias-suggestions/" + suggestion.getId() + "/dismiss")
                        .header("Authorization", bearer(engineerToken)))
                .andExpect(status().isConflict());
    }
}
