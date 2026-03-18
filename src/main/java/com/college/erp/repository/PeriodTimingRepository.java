package com.college.erp.repository;

import com.college.erp.model.PeriodTiming;
import com.college.erp.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PeriodTimingRepository extends JpaRepository<PeriodTiming, Long> {
    List<PeriodTiming> findByDepartmentOrderByPeriodNumberAsc(Department department);

    java.util.List<PeriodTiming> findByDepartmentAndPeriodNumber(Department department, int periodNumber);
}
