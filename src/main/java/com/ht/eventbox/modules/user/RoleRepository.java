package com.ht.eventbox.modules.user;

import com.ht.eventbox.entities.Role;
import com.ht.eventbox.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String roleName);
}
