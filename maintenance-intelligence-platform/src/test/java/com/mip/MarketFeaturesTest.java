package com.mip;

import com.mip.dictionary.entity.FailureCategory;
import com.mip.dictionary.entity.FailureMode;
import com.mip.machine.entity.Machine;
import com.mip.notification.entity.Notification;
import com.mip.notification.repository.NotificationRepository;
import com.mip.plant.entity.Plant;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.entity.RecordSource;
import com.mip.schedule.service.MaintenanceScheduleService;
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

/** Preventive maintenance schedules, CSV export, and the audit trail. */
class MarketFeaturesTest extends IntegrationTestBase {

    @Autowired
    private MaintenanceScheduleService scheduleService;
    @Autowired
    private NotificationRepository notificationRepository;

    private Plant plant;
    private Machine machine;
    private User engineer;
    private User admin;
    private String engineerToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        plant = newPlant();
        machine = newMachine(plant, newLine(plant), "PM-" + nextId(), "Pump Station");
        engineer = newUser(RoleName.ENGINEER, plant);
        admin = newUser(RoleName.ADMIN);
        engineerToken = login(engineer);
        adminToken = login(admin);
    }

    @Test
    void preventiveScheduleLifecycleAndOverdueSweep() throws Exception {
        // schedule already overdue (first due date in the past)
        String createBody = """
                {"machineId": %d, "title": "Monthly lubrication", "description": "Grease all points",
                 "intervalDays": 30, "firstDueOn": "%s"}
                """.formatted(machine.getId(), LocalDate.now().minusDays(3));
        MvcResult created = mockMvc.perform(post("/api/schedules")
                        .header("Authorization", bearer(engineerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OVERDUE"))
                .andReturn();
        long scheduleId = json(created).get("id").asLong();

        // it shows in the due list
        mockMvc.perform(get("/api/schedules/due").param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(engineerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(scheduleId));

        // the daily sweep notifies plant staff exactly once per day
        long before = notificationRepository.countByUserIdAndReadFlagFalse(engineer.getId());
        assertThat(scheduleService.notifyOverdueSchedules()).isEqualTo(1);
        assertThat(scheduleService.notifyOverdueSchedules()).isZero();
        assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(engineer.getId(),
                        PageRequest.of(0, 5)).getContent())
                .anyMatch(n -> n.getType() == Notification.NotificationType.MAINTENANCE_DUE);
        assertThat(notificationRepository.countByUserIdAndReadFlagFalse(engineer.getId()))
                .isEqualTo(before + 1);

        // completing writes a PREVENTIVE record and rolls the next due date forward
        mockMvc.perform(post("/api/schedules/" + scheduleId + "/complete")
                        .header("Authorization", bearer(engineerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"downtimeMinutes\": 20, \"notes\": \"all points greased\","
                                + "\"technician\": \"P. Mistry\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.lastPerformedOn").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.nextDueOn").value(LocalDate.now().plusDays(30).toString()));

        MaintenanceRecord record = recordRepository.findAll().stream()
                .filter(r -> r.getSource() == RecordSource.PREVENTIVE)
                .findFirst().orElseThrow();
        assertThat(record.getMachine().getId()).isEqualTo(machine.getId());
        assertThat(record.getDowntimeMinutes()).isEqualTo(20);
        assertThat(record.getDescription()).contains("Monthly lubrication");

        // viewers cannot create schedules; bad interval is a validation error
        User viewer = newUser(RoleName.VIEWER, plant);
        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", bearer(login(viewer)))
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", bearer(engineerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"machineId\":" + machine.getId()
                                + ",\"title\":\"Bad\",\"intervalDays\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordsExportAsCsvWithFiltersApplied() throws Exception {
        FailureMode bearing = ensureFailureMode("FM-BRG", "Bearing Failure",
                FailureCategory.MECHANICAL, "bearing");
        newRecord(plant, machine, bearing, LocalDate.now().minusDays(1), 90, engineer);

        MvcResult export = mockMvc.perform(get("/api/records/export")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(engineerToken)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Content-Disposition",
                                org.hamcrest.Matchers.containsString(".csv")))
                .andReturn();
        assertThat(export.getResponse().getContentType()).startsWith("text/csv");
        String csv = export.getResponse().getContentAsString();
        assertThat(csv).startsWith("id,date,machineCode");
        assertThat(csv).contains(machine.getCode());
        assertThat(csv).contains("Bearing Failure");

        // a filter that matches nothing still yields a valid header-only CSV
        MvcResult empty = mockMvc.perform(get("/api/records/export")
                        .param("plantId", plant.getId().toString())
                        .param("text", "no-such-text-anywhere")
                        .header("Authorization", bearer(engineerToken)))
                .andExpect(status().isOk()).andReturn();
        assertThat(empty.getResponse().getContentAsString().trim().split("\n")).hasSize(1);
    }

    @Test
    void auditTrailRecordsAdminAndDataActions() throws Exception {
        // trigger two audited actions
        mockMvc.perform(post("/api/machines")
                        .header("Authorization", bearer(engineerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantId\":" + plant.getId() + ",\"code\":\"AU-" + nextId()
                                + "\",\"name\":\"Audited Machine\"}"))
                .andExpect(status().isCreated());
        MaintenanceRecord record = newRecord(plant, machine, null, LocalDate.now(), 10, engineer);
        mockMvc.perform(post("/api/records/" + record.getId() + "/reject")
                        .header("Authorization", bearer(engineerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"test data\"}"))
                .andExpect(status().isOk());

        // admin sees them, newest first, filterable by action
        mockMvc.perform(get("/api/audit").param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.action=='MACHINE_CREATED')]").exists())
                .andExpect(jsonPath("$.content[?(@.action=='RECORD_REJECTED')]").exists());
        mockMvc.perform(get("/api/audit").param("action", "RECORD_REJECTED")
                        .param("plantId", plant.getId().toString())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actorName").value(engineer.getFullName()))
                .andExpect(jsonPath("$.content[0].entityId").value(record.getId()));

        // the trail is admin-only
        mockMvc.perform(get("/api/audit").header("Authorization", bearer(engineerToken)))
                .andExpect(status().isForbidden());
    }
}
