package com.college.erp.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String rollNumber;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true)
    private String email;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<Attendance> attendances;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<Marks> marks;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<FeeRecord> feeRecords;

    // Document Paths
    private String aadhaarCardPath;
    private String panCardPath;
    private String incomeCertificatePath;
    private String communityCertificatePath;
    private String bankPassbookPath;
    private String marksheetPath;
    private String profilePhotoPath;
    private String address;
    private String contactNumber;
    private String familyContactNumber;

    @Transient
    private String tempPassword;
}
