package com.mip.machine.repository;

import com.mip.machine.entity.MachineAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MachineAliasRepository extends JpaRepository<MachineAlias, Long> {

    Optional<MachineAlias> findByPlantIdAndAlias(Long plantId, String alias);

    List<MachineAlias> findByMachineIdOrderByAliasAsc(Long machineId);

    List<MachineAlias> findByPlantId(Long plantId);

    boolean existsByPlantIdAndAlias(Long plantId, String alias);
}
