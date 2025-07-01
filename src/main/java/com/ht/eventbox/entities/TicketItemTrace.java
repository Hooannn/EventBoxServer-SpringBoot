package com.ht.eventbox.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.enums.OrderStatus;
import com.ht.eventbox.enums.TicketItemTraceEvent;
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
@Table(name = "ticket_item_traces")
public class TicketItemTrace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_item_id", nullable = false)
    private TicketItem ticketItem;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "issuer_id", nullable = false)
    private User issuer;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TicketItemTraceEvent event;

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private java.time.LocalDateTime createdAt;
}
