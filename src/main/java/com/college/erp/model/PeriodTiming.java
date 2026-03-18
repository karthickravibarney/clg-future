package com.college.erp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "period_timings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodTiming {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int periodNumber; // 1-8

    @Column(nullable = false)
    private String startTime; // e.g., "09:00 AM"

    @Column(nullable = false)
    private String endTime; // e.g., "09:50 AM"

    private boolean isBreak;
    private String breakName; // e.g., "Short Break", "Lunch Break"

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}
