package com.ht.eventbox.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Checks;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.HashSet;
import java.util.Set;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tickets")
@Checks({
        @Check(constraints = "sale_start_time < sale_end_time"),
        @Check(constraints = "price >= 0"),
        @Check(constraints = "initial_stock >= 0"),
        @Check(constraints = "stock >= 0"),
})
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_show_id", nullable = false)
    @JsonProperty("event_show")
    @JsonBackReference
    private EventShow eventShow;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "initial_stock", nullable = false)
    @JsonProperty("initial_stock")
    private int initialStock;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "available", nullable = false)
    private boolean available;

    @Column(name = "sale_start_time", nullable = false)
    @JsonProperty("sale_start_time")
    private java.time.LocalDateTime saleStartTime;

    @Column(name = "sale_end_time", nullable = false)
    @JsonProperty("sale_end_time")
    private java.time.LocalDateTime saleEndTime;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "ticket_assets",
            joinColumns = @JoinColumn(name = "ticket_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "asset_id", nullable = false)
    )
    private Set<Asset> assets = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private java.time.LocalDateTime updatedAt;
}
