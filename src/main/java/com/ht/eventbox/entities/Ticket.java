package com.ht.eventbox.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Checks;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tickets")
@Checks({
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

    @JsonProperty("seatmap_block_id")
    @Column(name = "seatmap_block_id")
    private String seatmapBlockId;

    @Column(name = "description", columnDefinition = "TEXT")
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

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private java.time.LocalDateTime updatedAt;
}
