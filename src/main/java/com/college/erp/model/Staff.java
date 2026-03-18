package com.college.erp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "staff")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String employeeId;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true)
    private String email;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    private String designation;

    @Column(nullable = false)
    private String password;

    private String profilePhotoPath;
    private String address;
    private String contactNumber;
    private String familyContactNumber;
    private String aadhaarCardPath;
    private String panCardPath;
    private String bankPassbookPath;
    private String degreeCertificatePath;
    private String marksheetPath;
    private String uanDocumentPath;

    @Transient
    private String tempPassword;
}
