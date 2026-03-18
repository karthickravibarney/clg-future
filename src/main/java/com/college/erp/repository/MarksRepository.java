package com.college.erp.repository;

import com.college.erp.model.Marks;
import com.college.erp.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MarksRepository extends JpaRepository<Marks, Long> {
    List<Marks> findByStudent(Student student);
}
