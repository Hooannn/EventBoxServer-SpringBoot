package com.ht.eventbox.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ht.eventbox.enums.AssetUsage;
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
@Table(name = "assets")
public class Asset {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "signature", nullable = false)
    private String signature;

    @Column(name = "public_id", nullable = false, unique = true)
    @JsonProperty("public_id")
    private String publicId;

    @Column(name = "original_url", nullable = false)
    @JsonProperty("original_url")
    private String originalUrl;

    @Column(name = "secure_url", nullable = false)
    @JsonProperty("secure_url")
    private String secureUrl;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AssetUsage usage;

    @Column(name = "format", nullable = false)
    private String format;

    @Column(name = "resource_type", nullable = false)
    @JsonProperty("resource_type")
    private String resourceType;

    @Column(name = "folder", nullable = false)
    private String folder;

    @Column(name = "etag", nullable = false)
    @JsonProperty("etag")
    private String eTag;

    @Column(name = "width", nullable = false)
    private long width;

    @Column(name = "height", nullable = false)
    private long height;

    @Column(name = "bytes", nullable = false)
    private long bytes;

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private java.time.LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private java.time.LocalDateTime updatedAt;
}
