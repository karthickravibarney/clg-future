package com.college.erp.repository;

import com.college.erp.model.Batch;
import com.college.erp.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findByDepartment(Department department);
}
