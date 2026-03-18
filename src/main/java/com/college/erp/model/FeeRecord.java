package com.college.erp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fee_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String status; // PAID, PENDING, OVERDUE

    private LocalDateTime transactionDate;
}
