package com.college.erp.repository;

import com.college.erp.model.Attendance;
import com.college.erp.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByStudent(Student student);

    List<Attendance> findByStudentAndDate(Student student, LocalDate date);

    java.util.Optional<Attendance> findByStudentAndDateAndPeriodNumber(Student student, LocalDate date,
            int periodNumber);

    List<Attendance> findByMarkedBy(com.college.erp.model.Staff staff);
}
