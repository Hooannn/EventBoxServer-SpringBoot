package com.ht.eventbox.modules.user;

import com.ht.eventbox.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String username);

    List<User> findAllByOrderByIdAsc();

    Page<User> findAllByOrderByIdAsc(Pageable pageable);

    @Query("""
            SELECT u
            FROM User u
            WHERE :search IS NULL
               OR :search = ''
               OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
            ORDER BY u.id ASC
            """)
    Page<User> searchAllByOrderByIdAsc(@Param("search") String search, Pageable pageable);

    boolean existsByRolesId(Long roleId);
}
