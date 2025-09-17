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

import java.util.ArrayList;
import java.util.List;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_shows")
@Checks({
        @Check(constraints = "start_time < end_time"),
        @Check(constraints = "sale_start_time < sale_end_time"),
        @Check(constraints = "sale_start_time < end_time"),
        @Check(constraints = "sale_end_time < end_time"),
})
public class EventShow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    @JsonBackReference
    private Event event;

    @Column(name = "title", nullable = false)
    private String title;

    @JsonProperty("enabled_seatmap")
    @Column(name = "enabled_seatmap", nullable = false, columnDefinition = "boolean default false")
    private boolean enabledSeatmap = false;

    @Column(name = "seatmap", columnDefinition = "TEXT")
    private String seatmap;

    @JsonProperty("seatmap_svg")
    @Column(name = "seatmap_svg", columnDefinition = "TEXT")
    private String seatmapSvg;

    @Column(name = "start_time", nullable = false)
    @JsonProperty("start_time")
    private java.time.LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    @JsonProperty("end_time")
    private java.time.LocalDateTime endTime;

    @Column(name = "sale_start_time", nullable = false)
    @JsonProperty("sale_start_time")
    private java.time.LocalDateTime saleStartTime;

    @Column(name = "sale_end_time", nullable = false)
    @JsonProperty("sale_end_time")
    private java.time.LocalDateTime saleEndTime;

    @JsonManagedReference
    @OneToMany(mappedBy = "eventShow", targetEntity = Ticket.class, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Ticket> tickets = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private java.time.LocalDateTime updatedAt;
}
