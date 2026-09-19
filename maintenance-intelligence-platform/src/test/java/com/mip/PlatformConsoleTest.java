package com.mip;

import com.fasterxml.jackson.databind.JsonNode;
import com.mip.machine.entity.Machine;
import com.mip.plant.entity.Organisation;
import com.mip.plant.entity.Plant;
import com.mip.plant.repository.OrganisationRepository;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The software owner's cross-organisation console and the PLATFORM_ADMIN role. */
class PlatformConsoleTest extends IntegrationTestBase {

    @Autowired
    private OrganisationRepository organisationRepository;

    private Organisation organisation;
    private Plant plantA;
    private Plant plantB;
    private User owner;
    private User customerAdmin;
    private User engineer;
    private String ownerToken;

    @BeforeEach
    void setUp() throws Exception {
        organisation = organisationRepository.save(
                new Organisation("ORG" + nextId(), "Acme Manufacturing " + nextId()));
        plantA = newPlant();
        plantA.setOrganisation(organisation);
        plantA = plantRepository.save(plantA);
        plantB = newPlant();
        plantB.setOrganisation(organisation);
        plantB = plantRepository.save(plantB);

        Machine machineA = newMachine(plantA, null, "PC-" + nextId(), "Console Machine A");
        newMachine(plantB, null, "PC-" + nextId(), "Console Machine B");
        engineer = newUser(RoleName.ENGINEER, plantA, plantB);
        newRecord(plantA, machineA, null, LocalDate.now().minusDays(2), 120, engineer);

        owner = newUser(RoleName.PLATFORM_ADMIN);
        customerAdmin = newUser(RoleName.ADMIN);
        ownerToken = login(owner);
    }

    @Test
    void ownerSeesCrossOrganisationUsage() throws Exception {
        MvcResult overview = mockMvc.perform(get("/api/platform/overview")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode stats = json(overview);
        assertThat(stats.get("organisations").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(stats.get("plants").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(stats.get("machines").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(stats.get("maintenanceRecords").asLong()).isGreaterThanOrEqualTo(1);

        // per-organisation row carries this org's usage
        MvcResult orgs = mockMvc.perform(get("/api/platform/organisations")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode row = null;
        for (JsonNode candidate : json(orgs)) {
            if (candidate.get("id").asLong() == organisation.getId()) {
                row = candidate;
            }
        }
        assertThat(row).isNotNull();
        assertThat(row.get("plantCount").asLong()).isEqualTo(2);
        assertThat(row.get("machineCount").asLong()).isEqualTo(2);
        assertThat(row.get("recordCount").asLong()).isEqualTo(1);
        assertThat(row.get("userCount").asLong()).isEqualTo(1);
        assertThat(row.get("lastActivityAt").isNull()).isFalse();

        // drill-down: per-plant stats and the org's users
        mockMvc.perform(get("/api/platform/organisations/" + organisation.getId())
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plants.length()").value(2))
                .andExpect(jsonPath("$.plants[?(@.id == " + plantA.getId()
                        + ")].downtimeLast30DaysMinutes").value(120))
                .andExpect(jsonPath("$.users[0].email").value(engineer.getEmail()));
    }

    @Test
    void ownerInheritsAdminPowersButAdminsCannotSeeThePlatform() throws Exception {
        // role hierarchy: the owner passes admin-only and plant-scoped checks everywhere
        mockMvc.perform(get("/api/users").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/plants/" + plantA.getId() + "/settings")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/audit").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());

        // a customer admin never sees across organisations
        String adminToken = login(customerAdmin);
        mockMvc.perform(get("/api/platform/overview").header("Authorization", bearer(adminToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/platform/organisations")
                        .header("Authorization", bearer(login(engineer))))
                .andExpect(status().isForbidden());
    }

    @Test
    void consolePageIsServed() throws Exception {
        mockMvc.perform(get("/admin.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Platform Console")));
    }
}
