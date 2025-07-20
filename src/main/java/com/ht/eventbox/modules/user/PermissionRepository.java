package com.ht.eventbox.modules.user;

import com.ht.eventbox.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String permissionName);

    List<Permission> findAllByOrderByIdAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdIsNot(String name, Long permissionId);
}
