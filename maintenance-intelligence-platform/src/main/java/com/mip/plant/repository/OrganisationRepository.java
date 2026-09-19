package com.mip.plant.repository;

import com.mip.plant.entity.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganisationRepository extends JpaRepository<Organisation, Long> {

    Optional<Organisation> findByCode(String code);
}
