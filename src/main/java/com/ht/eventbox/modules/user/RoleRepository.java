package com.ht.eventbox.modules.user;

import com.ht.eventbox.entities.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String roleName);

    List<Role> findAllByOrderByIdAsc();

    Page<Role> findAllByOrderByIdAsc(Pageable pageable);

    boolean existsByName(String name);

    Optional<Role> findByIdAndNameIsNot(Long roleId, String name);

    boolean existsByPermissionsId(Long permissionId);

    boolean existsByNameAndIdIsNot(String name, Long roleId);
}
