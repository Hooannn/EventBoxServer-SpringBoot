package com.ht.eventbox.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "name_vi", nullable = false)
    @JsonProperty("name_vi")
    private String nameVi;

    @Column(name = "name_en", nullable = false)
    @JsonProperty("name_en")
    private String nameEn;

    @Column(name = "featured", nullable = false, columnDefinition = "boolean default false")
    private boolean featured = false;

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private java.time.LocalDateTime updatedAt;
}
