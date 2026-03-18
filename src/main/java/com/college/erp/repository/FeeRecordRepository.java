package com.college.erp.repository;

import com.college.erp.model.FeeRecord;
import com.college.erp.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {
    List<FeeRecord> findByStudent(Student student);
}
