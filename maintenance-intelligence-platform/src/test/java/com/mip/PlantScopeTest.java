package com.mip;

import com.mip.machine.entity.Machine;
import com.mip.plant.entity.Plant;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlantScopeTest extends IntegrationTestBase {

    private Plant myPlant;
    private Plant otherPlant;
    private User engineer;
    private User viewer;
    private User admin;
    private Machine otherMachine;

    @BeforeEach
    void setUp() {
        myPlant = newPlant();
        otherPlant = newPlant();
        engineer = newUser(RoleName.ENGINEER, myPlant);
        viewer = newUser(RoleName.VIEWER, myPlant);
        admin = newUser(RoleName.ADMIN);
        otherMachine = newMachine(otherPlant, null, "OM-" + nextId(), "Other Machine");
    }

    @Test
    void foreignPlantIsInvisibleNotForbidden() throws Exception {
        String token = login(engineer);
        mockMvc.perform(get("/api/plants/" + otherPlant.getId() + "/settings")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/machines/" + otherMachine.getId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/records").param("plantId", otherPlant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminSeesEveryPlantWithoutAssignment() throws Exception {
        String token = login(admin);
        mockMvc.perform(get("/api/plants/" + otherPlant.getId() + "/settings")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plantId").value(otherPlant.getId()));
    }

    @Test
    void plantListIsScopedToAssignments() throws Exception {
        String token = login(engineer);
        mockMvc.perform(get("/api/plants").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + myPlant.getId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + otherPlant.getId() + ")]").doesNotExist());
    }

    @Test
    void viewerCannotWriteButCanRead() throws Exception {
        Machine machine = newMachine(myPlant, null, "VM-" + nextId(), "Viewer Machine");
        String token = login(viewer);

        mockMvc.perform(get("/api/machines").param("plantId", myPlant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        String body = """
                {"plantId": %d, "machineId": %d, "recordDate": "%s",
                 "downtimeMinutes": 30, "description": "viewer tries to write"}
                """.formatted(myPlant.getId(), machine.getId(), LocalDate.now());
        mockMvc.perform(post("/api/records")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }
}
