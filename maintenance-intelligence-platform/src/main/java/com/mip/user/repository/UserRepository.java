package com.mip.user.repository;

import com.mip.user.entity.RoleName;
import com.mip.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByOrderByFullNameAsc();

    List<User> findByActiveTrueAndPlantsId(Long plantId);

    List<User> findByActiveTrueAndRole(RoleName role);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query("select count(distinct u) from User u join u.plants p where p.id in :plantIds")
    long countDistinctByPlantIds(@Param("plantIds") List<Long> plantIds);

    @Query("select distinct u from User u join u.plants p where p.id in :plantIds order by u.fullName")
    List<User> findDistinctByPlantIds(@Param("plantIds") List<Long> plantIds);
}
