package com.college.erp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Marks {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private int semester;

    @Column(nullable = false)
    private double marksObtained;

    @Column(nullable = false)
    private double totalMarks;
}
