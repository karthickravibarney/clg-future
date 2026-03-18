package com.college.erp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "timetable_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String dayOfWeek; // MONDAY, TUESDAY, etc.

    @Column(nullable = false)
    private int periodNumber; // 1-8

    private Integer year; // 1, 2, 3, 4
    private Integer semester; // 1-8

    private String subjectName;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;
}
