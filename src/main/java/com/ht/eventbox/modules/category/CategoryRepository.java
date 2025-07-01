package com.ht.eventbox.modules.category;

import com.ht.eventbox.entities.Category;
import com.ht.eventbox.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

}
