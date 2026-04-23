package com.northcare.hospitalcore.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "ward_type", nullable = false, length = 20)
    private WardType wardType;

    @Column(name = "floor", nullable = false)
    private int floor;

    @Column(name = "bed_count", nullable = false)
    private int bedCount;

    @Column(name = "available_beds", nullable = false)
    private int availableBeds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum WardType {
        ICU, GENERAL, PEDIATRIC, EMERGENCY
    }
}
