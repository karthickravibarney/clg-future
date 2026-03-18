package com.college.erp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private LocalDateTime examTime;

    @Column(nullable = false)
    private String venue;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;
}
