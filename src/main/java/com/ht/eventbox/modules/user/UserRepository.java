package com.ht.eventbox.modules.user;

import com.ht.eventbox.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String username);

    List<User> findAllByOrderByIdAsc();

    Page<User> findAllByOrderByIdAsc(Pageable pageable);

    boolean existsByRolesId(Long roleId);
}
