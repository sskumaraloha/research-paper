package com.mip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mip.dictionary.entity.FailureCategory;
import com.mip.dictionary.entity.FailureMode;
import com.mip.dictionary.repository.FailureModeRepository;
import com.mip.machine.entity.Criticality;
import com.mip.machine.entity.Machine;
import com.mip.machine.repository.MachineRepository;
import com.mip.plant.entity.Plant;
import com.mip.plant.entity.ProductionLine;
import com.mip.plant.repository.PlantRepository;
import com.mip.plant.repository.ProductionLineRepository;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.entity.RecordSource;
import com.mip.record.repository.MaintenanceRecordRepository;
import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import com.mip.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "Test@1234";
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected PlantRepository plantRepository;
    @Autowired
    protected ProductionLineRepository lineRepository;
    @Autowired
    protected MachineRepository machineRepository;
    @Autowired
    protected FailureModeRepository failureModeRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected MaintenanceRecordRepository recordRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected int nextId() {
        return SEQUENCE.incrementAndGet();
    }

    protected Plant newPlant() {
        return plantRepository.save(new Plant("PL" + nextId(), "Plant " + nextId(), "Test City"));
    }

    protected ProductionLine newLine(Plant plant) {
        return lineRepository.save(new ProductionLine(plant, "L" + nextId(), "Line " + nextId()));
    }

    protected Machine newMachine(Plant plant, ProductionLine line, String code, String name) {
        return machineRepository.save(new Machine(plant, line, code, name, Criticality.HIGH));
    }

    protected User newUser(RoleName role, Plant... plants) {
        User user = new User("User " + nextId(), "user" + nextId() + "@test.local",
                passwordEncoder.encode(PASSWORD), role);
        for (Plant plant : plants) {
            user.getPlants().add(plant);
        }
        return userRepository.save(user);
    }

    /** Failure modes are global; create-once semantics keep test classes independent. */
    protected FailureMode ensureFailureMode(String code, String name, FailureCategory category,
                                            String keywords) {
        return failureModeRepository.findByCodeIgnoreCase(code)
                .orElseGet(() -> failureModeRepository.save(
                        new FailureMode(code, name, category, keywords)));
    }

    protected MaintenanceRecord newRecord(Plant plant, Machine machine, FailureMode mode,
                                          LocalDate date, int downtimeMinutes, User creator) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setPlant(plant);
        record.setMachine(machine);
        record.setFailureMode(mode);
        record.setRecordDate(date);
        record.setDowntimeMinutes(downtimeMinutes);
        record.setDescription(mode == null ? "General maintenance" : mode.getName() + " on "
                + machine.getName());
        record.setTechnician("T. Tester");
        record.setSource(RecordSource.MANUAL);
        record.setCreatedBy(creator);
        return recordRepository.save(record);
    }

    protected String login(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\""
                                + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("accessToken").asText();
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
