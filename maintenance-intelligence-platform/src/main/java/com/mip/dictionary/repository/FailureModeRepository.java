package com.mip.dictionary.repository;

import com.mip.dictionary.entity.FailureMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FailureModeRepository extends JpaRepository<FailureMode, Long> {

    Optional<FailureMode> findByCodeIgnoreCase(String code);

    Optional<FailureMode> findByNameIgnoreCase(String name);

    List<FailureMode> findAllByOrderByNameAsc();
}
