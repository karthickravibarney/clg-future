package com.college.erp.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., "2022-2026"

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    private List<Student> students;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    private List<TimetableEntry> timetableEntries;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL)
    private List<ExamSchedule> examSchedules;
}
