package com.mip;

import com.mip.dictionary.entity.FailureCategory;
import com.mip.dictionary.entity.FailureMode;
import com.mip.dictionary.entity.SearchSynonym;
import com.mip.dictionary.entity.SynonymDomain;
import com.mip.dictionary.repository.SearchSynonymRepository;
import com.mip.machine.entity.Machine;
import com.mip.plant.entity.Plant;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssistantSearchTest extends IntegrationTestBase {

    @Autowired
    private SearchSynonymRepository synonymRepository;

    private Plant plant;
    private Machine lathe;
    private User engineer;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        plant = newPlant();
        lathe = newMachine(plant, null, "CNC-01", "CNC Lathe 01");
        Machine press = newMachine(plant, null, "HP-300", "Hydraulic Press");
        engineer = newUser(RoleName.ENGINEER, plant);
        FailureMode bearing = ensureFailureMode("FM-BRG", "Bearing Failure",
                FailureCategory.MECHANICAL, "bearing");
        FailureMode hydraulic = ensureFailureMode("FM-HYD", "Hydraulic Leak",
                FailureCategory.HYDRAULIC, "hydraulic,oil leak");
        if (synonymRepository.findByDomainAndTerm(SynonymDomain.FAILURE_MODE, "brg").isEmpty()) {
            synonymRepository.save(new SearchSynonym(SynonymDomain.FAILURE_MODE, "brg", "bearing"));
        }

        LocalDate today = LocalDate.now();
        newRecord(plant, lathe, bearing, today.minusDays(3), 200, engineer);
        newRecord(plant, lathe, bearing, today.minusDays(10), 150, engineer);
        newRecord(plant, press, hydraulic, today.minusDays(6), 60, engineer);
        token = login(engineer);
    }

    private void ask(String question, String expectedIntent) throws Exception {
        mockMvc.perform(post("/api/assistant/ask")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantId\":" + plant.getId() + ",\"question\":\""
                                + question + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value(expectedIntent));
    }

    @Test
    void questionsRouteToTheRightIntent() throws Exception {
        ask("Show me the history of CNC-01", "MACHINE_HISTORY");
        ask("Which machine has the highest downtime?", "HIGHEST_DOWNTIME");
        ask("How much downtime did bearing failure cause?", "FAILURE_MODE_DOWNTIME");
        ask("What keeps failing repeatedly in the plant?", "REPEATED_FAILURES");
        ask("Show plant KPIs", "PLANT_KPIS");
        ask("zzz", "FALLBACK");
    }

    @Test
    void machineHistoryAnswerCarriesRealData() throws Exception {
        mockMvc.perform(post("/api/assistant/ask")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantId\":" + plant.getId()
                                + ",\"question\":\"Show me the history of CNC-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(
                        org.hamcrest.Matchers.containsString("CNC Lathe 01")))
                .andExpect(jsonPath("$.tiles[0].value").value("2"))
                .andExpect(jsonPath("$.records.length()").value(2));
    }

    @Test
    void synonymsWidenTheSearch() throws Exception {
        // records say "Bearing Failure ..." but the query says "brg"
        mockMvc.perform(get("/api/search/records").param("query", "brg")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/search").param("query", "cnc")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.machines[0].code").value("CNC-01"));
    }

    @Test
    void suggestionsComeFromTheData() throws Exception {
        mockMvc.perform(get("/api/assistant/suggestions")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(
                        org.hamcrest.Matchers.containsString("CNC Lathe 01")));
    }
}
