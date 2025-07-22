package com.ht.eventbox.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.enums.EventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @JsonManagedReference
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startTime ASC")
    private List<EventShow> shows = new ArrayList<>();

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "place_name", nullable = false)
    @JsonProperty("place_name")
    private String placeName;

    @Column(name = "published_at")
    @JsonProperty("published_at")
    private java.time.LocalDateTime publishedAt;

    @Column(name = "payout_at")
    @JsonProperty("payout_at")
    private java.time.LocalDateTime payoutAt;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "event_assets",
            joinColumns = @JoinColumn(name = "event_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "asset_id", nullable = false)
    )
    private Set<Asset> assets = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "event_categories",
            joinColumns = @JoinColumn(name = "event_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "category_id", nullable = false)
    )
    private Set<Category> categories = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "event_keywords",
            joinColumns = @JoinColumn(name = "event_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "keyword_id", nullable = false)
    )
    private Set<Keyword> keywords = new HashSet<>();

    @Column(name = "featured", nullable = false, columnDefinition = "boolean default false")
    private boolean featured = false;

    @Column(name = "trending", nullable = false, columnDefinition = "boolean default false")
    private boolean trending = false;

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private java.time.LocalDateTime updatedAt;
}
