package com.college.erp.repository;

import com.college.erp.model.Batch;
import com.college.erp.model.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    List<ExamSchedule> findByBatch(Batch batch);
}
