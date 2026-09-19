package com.mip.config;

import com.mip.common.util.TextNormalizer;
import com.mip.dictionary.entity.FailureCategory;
import com.mip.dictionary.entity.FailureMode;
import com.mip.dictionary.entity.SearchSynonym;
import com.mip.dictionary.entity.SynonymDomain;
import com.mip.dictionary.repository.FailureModeRepository;
import com.mip.dictionary.repository.SearchSynonymRepository;
import com.mip.machine.entity.AliasSource;
import com.mip.machine.entity.Criticality;
import com.mip.machine.entity.Machine;
import com.mip.machine.entity.MachineAlias;
import com.mip.machine.repository.MachineAliasRepository;
import com.mip.machine.repository.MachineRepository;
import com.mip.part.entity.SparePart;
import com.mip.part.repository.SparePartRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Development seed: two plants with lines, machines, aliases, a failure-mode dictionary,
 * synonyms, spare parts, users for each role and six months of maintenance history.
 * Never runs in prod (dev profile only) and never re-seeds a populated database.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final PlantRepository plantRepository;
    private final ProductionLineRepository lineRepository;
    private final MachineRepository machineRepository;
    private final MachineAliasRepository aliasRepository;
    private final FailureModeRepository failureModeRepository;
    private final SearchSynonymRepository synonymRepository;
    private final SparePartRepository sparePartRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (plantRepository.count() > 0) {
            log.info("Seed skipped: database already populated");
            return;
        }
        seed();
        log.info("Development data seeded");
    }

    private void seed() {
        Plant pune = plantRepository.save(new Plant("PUNE", "Pune Plant", "Pune, MH"));
        Plant nashik = plantRepository.save(new Plant("NASHIK", "Nashik Plant", "Nashik, MH"));

        ProductionLine puneA = lineRepository.save(new ProductionLine(pune, "L1", "Assembly Line 1"));
        ProductionLine puneB = lineRepository.save(new ProductionLine(pune, "L2", "Machining Line"));
        ProductionLine nashikA = lineRepository.save(new ProductionLine(nashik, "L1", "Packaging Line"));

        List<FailureMode> failureModes = failureModeRepository.saveAll(List.of(
                new FailureMode("FM-BRG", "Bearing Failure", FailureCategory.MECHANICAL,
                        "bearing,brg,bearing seized,bearing noise,bearing worn"),
                new FailureMode("FM-BELT", "Belt Breakage", FailureCategory.MECHANICAL,
                        "belt,belt snapped,belt broke,v-belt,conveyor belt"),
                new FailureMode("FM-MOT", "Motor Burnout", FailureCategory.ELECTRICAL,
                        "motor,motor burnt,winding,motor tripped,burnout"),
                new FailureMode("FM-HYD", "Hydraulic Leak", FailureCategory.HYDRAULIC,
                        "hydraulic,oil leak,hose,cylinder leak,hyd leak"),
                new FailureMode("FM-PNE", "Pneumatic Failure", FailureCategory.PNEUMATIC,
                        "pneumatic,air leak,compressor,air pressure,valve stuck"),
                new FailureMode("FM-SEN", "Sensor Malfunction", FailureCategory.INSTRUMENTATION,
                        "sensor,proximity,limit switch,encoder,photo eye"),
                new FailureMode("FM-LUB", "Lubrication Issue", FailureCategory.LUBRICATION,
                        "lubrication,grease,oil level,dry running,no lube"),
                new FailureMode("FM-OVH", "Overheating", FailureCategory.PROCESS,
                        "overheat,temperature high,too hot,thermal trip,cooling"),
                new FailureMode("FM-ALN", "Misalignment", FailureCategory.MECHANICAL,
                        "misalignment,alignment,vibration,wobble,shaft out"),
                new FailureMode("FM-ELE", "Electrical Fault", FailureCategory.ELECTRICAL,
                        "electrical,short circuit,fuse,breaker,cable,wiring")));

        synonymRepository.saveAll(List.of(
                new SearchSynonym(SynonymDomain.FAILURE_MODE, "brg failure", "bearing failure"),
                new SearchSynonym(SynonymDomain.FAILURE_MODE, "bearing gone", "bearing failure"),
                new SearchSynonym(SynonymDomain.FAILURE_MODE, "belt cut", "belt breakage"),
                new SearchSynonym(SynonymDomain.FAILURE_MODE, "motor fail", "motor burnout"),
                new SearchSynonym(SynonymDomain.FAILURE_MODE, "oil leaking", "hydraulic leak"),
                new SearchSynonym(SynonymDomain.MACHINE, "cnc 1", "cnc 01"),
                new SearchSynonym(SynonymDomain.MACHINE, "big press", "hp 300"),
                new SearchSynonym(SynonymDomain.GENERAL, "breakdown", "failure"),
                new SearchSynonym(SynonymDomain.GENERAL, "downtime", "failure")));

        Machine cnc1 = machine(pune, puneB, "CNC-01", "CNC Lathe 01", "Ace Micromatic", "Jobber XL",
                Criticality.HIGH);
        Machine cnc2 = machine(pune, puneB, "CNC-02", "CNC Lathe 02", "Ace Micromatic", "Jobber XL",
                Criticality.MEDIUM);
        Machine press = machine(pune, puneA, "HP-300", "Hydraulic Press 300T", "Godrej", "HP300",
                Criticality.CRITICAL);
        Machine convA = machine(pune, puneA, "CONV-A", "Conveyor A", "FlexLink", "X85",
                Criticality.MEDIUM);
        Machine weld = machine(pune, puneA, "WLD-01", "Welding Robot 01", "KUKA", "KR16",
                Criticality.HIGH);
        Machine packer = machine(nashik, nashikA, "PKG-01", "Case Packer 01", "Bosch", "Elematic",
                Criticality.HIGH);
        Machine wrapper = machine(nashik, nashikA, "WRP-01", "Pallet Wrapper", "Signode", "Octopus",
                Criticality.LOW);

        alias(cnc1, "cnc lathe no 1");
        alias(cnc1, "lathe 1");
        alias(press, "press 300");
        alias(press, "hyd press");
        alias(convA, "main conveyor");
        alias(packer, "case packer");

        sparePartRepository.saveAll(List.of(
                new SparePart("BRG-6205", "Ball Bearing 6205", "Bearings"),
                new SparePart("BRG-6308", "Ball Bearing 6308", "Bearings"),
                new SparePart("BLT-V12", "V-Belt B47", "Belts"),
                new SparePart("MTR-5HP", "Induction Motor 5HP", "Motors"),
                new SparePart("SEAL-HYD", "Hydraulic Seal Kit", "Hydraulics"),
                new SparePart("SEN-PROX", "Proximity Sensor M18", "Sensors"),
                new SparePart("FLT-AIR", "Air Filter Element", "Pneumatics"),
                new SparePart("GRS-EP2", "Grease EP2 Cartridge", "Lubrication")));

        User admin = new User("Asha Admin", "admin@mip.local",
                passwordEncoder.encode("Admin@123"), RoleName.ADMIN);
        User demo = new User("Dev Engineer", "demo@mip.local",
                passwordEncoder.encode("Demo@123"), RoleName.ENGINEER);
        demo.getPlants().add(pune);
        demo.getPlants().add(nashik);
        User viewer = new User("Vikram Viewer", "viewer@mip.local",
                passwordEncoder.encode("Viewer@123"), RoleName.VIEWER);
        viewer.getPlants().add(pune);
        userRepository.saveAll(List.of(admin, demo, viewer));

        seedHistory(List.of(cnc1, cnc2, press, convA, weld, packer, wrapper), failureModes, demo);
    }

    private void seedHistory(List<Machine> machines, List<FailureMode> failureModes, User creator) {
        // deterministic pseudo-random history: repeated bearing failures on CNC-01,
        // chronic hydraulic leaks on the press, scattered noise elsewhere
        Random random = new Random(42);
        LocalDate today = LocalDate.now();
        String[] technicians = {"R. Kulkarni", "S. Patil", "M. Deshmukh", "A. Shaikh"};

        for (Machine machine : machines) {
            int events = switch (machine.getCode()) {
                case "CNC-01" -> 14;
                case "HP-300" -> 11;
                case "CONV-A" -> 8;
                default -> 4 + random.nextInt(3);
            };
            for (int i = 0; i < events; i++) {
                FailureMode mode = pickMode(machine, failureModes, random);
                MaintenanceRecord record = new MaintenanceRecord();
                record.setPlant(machine.getPlant());
                record.setMachine(machine);
                record.setFailureMode(mode);
                record.setRecordDate(today.minusDays(random.nextInt(180)));
                record.setDowntimeMinutes(30 + random.nextInt(420));
                record.setDescription(mode.getName() + " reported on " + machine.getName()
                        + "; " + "line stopped and maintenance called");
                record.setActionTaken("Inspected, replaced faulty component and test ran the machine");
                record.setTechnician(technicians[random.nextInt(technicians.length)]);
                record.setSource(RecordSource.MANUAL);
                record.setCreatedBy(creator);
                recordRepository.save(record);
            }
        }
    }

    private FailureMode pickMode(Machine machine, List<FailureMode> modes, Random random) {
        return switch (machine.getCode()) {
            case "CNC-01" -> random.nextInt(10) < 6 ? byCode(modes, "FM-BRG")
                    : modes.get(random.nextInt(modes.size()));
            case "HP-300" -> random.nextInt(10) < 5 ? byCode(modes, "FM-HYD")
                    : modes.get(random.nextInt(modes.size()));
            case "CONV-A" -> random.nextInt(10) < 4 ? byCode(modes, "FM-BELT")
                    : modes.get(random.nextInt(modes.size()));
            default -> modes.get(random.nextInt(modes.size()));
        };
    }

    private FailureMode byCode(List<FailureMode> modes, String code) {
        return modes.stream().filter(m -> m.getCode().equals(code)).findFirst().orElseThrow();
    }

    private Machine machine(Plant plant, ProductionLine line, String code, String name,
                            String manufacturer, String model, Criticality criticality) {
        Machine machine = new Machine(plant, line, code, name, criticality);
        machine.setManufacturer(manufacturer);
        machine.setModel(model);
        machine.setCommissionedOn(LocalDate.now().minusYears(3));
        return machineRepository.save(machine);
    }

    private void alias(Machine machine, String alias) {
        aliasRepository.save(new MachineAlias(machine, TextNormalizer.normalize(alias), AliasSource.SEED));
    }
}
