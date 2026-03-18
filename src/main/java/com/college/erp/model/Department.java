package com.college.erp.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int maxYears = 4;

    @Column(nullable = false)
    private int maxSemesters = 8;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private List<Batch> batches;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private List<Staff> staff;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    private List<PeriodTiming> periodTimings;
}
