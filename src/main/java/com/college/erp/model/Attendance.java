package com.college.erp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int periodNumber; // 1-8

    @Column(nullable = false)
    private boolean isPresent;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff markedBy;
}
