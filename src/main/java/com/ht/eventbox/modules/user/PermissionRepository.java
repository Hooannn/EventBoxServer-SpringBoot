package com.ht.eventbox.modules.user;

import com.ht.eventbox.entities.Permission;
import com.ht.eventbox.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String permissionName);
}
