package com.college.erp.repository;

import com.college.erp.model.TimetableEntry;
import com.college.erp.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
    List<TimetableEntry> findByBatchAndYearAndSemesterOrderByDayOfWeekAscPeriodNumberAsc(Batch batch, Integer year,
            Integer semester);

    List<TimetableEntry> findByBatchAndDayOfWeekOrderByPeriodNumberAsc(Batch batch, String dayOfWeek);

    List<TimetableEntry> findByStaff(com.college.erp.model.Staff staff);

    java.util.Optional<TimetableEntry> findByBatchAndYearAndSemesterAndDayOfWeekAndPeriodNumber(Batch batch,
            Integer year, Integer semester, String dayOfWeek,
            int periodNumber);
}
