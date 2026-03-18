package com.college.erp.repository;

import com.college.erp.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByEmployeeId(String employeeId);

    long countByDepartment(com.college.erp.model.Department department);
}
