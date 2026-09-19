package com.mip.part.repository;

import com.mip.part.entity.SparePart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SparePartRepository extends JpaRepository<SparePart, Long> {

    Optional<SparePart> findByPartNumberIgnoreCase(String partNumber);

    Optional<SparePart> findByNameIgnoreCase(String name);

    @Query("""
            select p from SparePart p
            where (:query is null
                   or lower(p.name) like lower(concat('%', :query, '%'))
                   or lower(p.partNumber) like lower(concat('%', :query, '%')))
            """)
    Page<SparePart> search(@Param("query") String query, Pageable pageable);
}
