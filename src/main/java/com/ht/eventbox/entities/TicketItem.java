package com.ht.eventbox.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.enums.FeedbackSentimentType;
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
@Table(name = "ticket_items", indexes = {
        @Index(name = "idx_ticket_item_order_id", columnList = "order_id"),
        @Index(name = "idx_ticket_item_ticket_id", columnList = "ticket_id")
})
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

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(length = 20, name = "feedback_type")
    @Enumerated(EnumType.STRING)
    @JsonProperty("feedback_type")
    private FeedbackSentimentType feedbackType;

    @Column(name = "reminded", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean reminded = false;

    @Column(name = "place_total", nullable = false)
    @JsonProperty("place_total")
    private Double placeTotal;

    @JsonManagedReference
    @OneToMany(mappedBy = "ticketItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<TicketItemTrace> traces = new ArrayList<>();

    @Column(name = "feedback_at")
    @JsonProperty("feedback_at")
    private java.time.LocalDateTime feedbackAt;

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private java.time.LocalDateTime updatedAt;
}
