package com.college.erp.service;

import com.college.erp.model.Department;
import com.college.erp.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
    }

    public Department createDepartment(Department department) {
        return departmentRepository.save(department);
    }

    public void updateDepartment(Long id, Department department) {
        Department existing = getDepartmentById(id);
        existing.setName(department.getName());
        existing.setCode(department.getCode());
        departmentRepository.save(existing);
    }

    public void deleteDepartment(Long id) {
        departmentRepository.deleteById(id);
    }
}
