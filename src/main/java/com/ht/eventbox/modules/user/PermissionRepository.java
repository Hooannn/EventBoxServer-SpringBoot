package com.ht.eventbox.modules.user;

import com.ht.eventbox.entities.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String permissionName);

    List<Permission> findAllByOrderByIdAsc();

    Page<Permission> findAllByOrderByIdAsc(Pageable pageable);

    @Query("""
            SELECT p
            FROM Permission p
            WHERE :search IS NULL
               OR :search = ''
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY p.id ASC
            """)
    Page<Permission> searchAllByOrderByIdAsc(@Param("search") String search, Pageable pageable);

    boolean existsByName(String name);

    boolean existsByNameAndIdIsNot(String name, Long permissionId);
}
