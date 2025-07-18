package com.ht.eventbox.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
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
@Table(name = "ticket_items")
public class TicketItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "place_total", nullable = false)
    @JsonProperty("place_total")
    private Double placeTotal;

    @JsonManagedReference
    @OneToMany(mappedBy = "ticketItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<TicketItemTrace> traces = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private java.time.LocalDateTime updatedAt;
}
