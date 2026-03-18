package com.college.erp.service;

import com.college.erp.model.Student;
import com.college.erp.repository.StudentRepository;
import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final com.college.erp.repository.DepartmentRepository departmentRepository;

    public StudentService(StudentRepository studentRepository,
            com.college.erp.repository.DepartmentRepository departmentRepository) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
    }

    public Student createStudent(Student student) {
        // Generate Roll Number: STU + Year + DeptCode + Sequence
        String year = String.valueOf(java.time.Year.now().getValue());
        String deptCode = departmentRepository.findById(student.getDepartment().getId())
                .orElseThrow(() -> new RuntimeException("Department not found")).getCode();
        
        String prefix = String.format("STU%s%s", year, deptCode);
        List<Student> studentsWithPrefix = studentRepository.findAll().stream()
                .filter(s -> s.getRollNumber().startsWith(prefix))
                .collect(java.util.stream.Collectors.toList());
        
        long nextSeq = 1;
        if (!studentsWithPrefix.isEmpty()) {
            nextSeq = studentsWithPrefix.stream()
                    .map(s -> {
                        try {
                            return Long.parseLong(s.getRollNumber().substring(prefix.length()));
                        } catch (Exception e) {
                            return 0L;
                        }
                    })
                    .max(Long::compare)
                    .orElse(0L) + 1;
        }
        String rollNumber = String.format("%s%03d", prefix, nextSeq);

        // Generate Random Password
        String generatedPassword = generateRandomPassword(8);

        student.setRollNumber(rollNumber);
        student.setPassword(generatedPassword);
        student.setTempPassword(generatedPassword);

        Student savedStudent = studentRepository.save(student);
        // In a real system, you would email/print the 'generatedPassword' to the user
        // here.
        System.out.println("Generated Credentials for " + savedStudent.getFullName() +
                ": Roll=" + rollNumber + ", Temp Password=" + generatedPassword);

        return savedStudent;
    }

    public Student editStudent(Long id, Student updatedStudent) {
        Student existingStudent = studentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        existingStudent.setFullName(updatedStudent.getFullName());
        existingStudent.setEmail(updatedStudent.getEmail());
        existingStudent.setDepartment(updatedStudent.getDepartment());
        existingStudent.setBatch(updatedStudent.getBatch());
        // Do not update password or roll number here
        return studentRepository.save(existingStudent);
    }

    public Student updateStudent(Student student) {
        return studentRepository.save(student);
    }

    public void deleteStudent(Long id) {
        Student existingStudent = studentRepository.findById(id).orElse(null);
        if (existingStudent != null) {
            studentRepository.delete(existingStudent);
        }
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public java.util.Optional<Student> getStudentByRollNumber(String rollNumber) {
        return studentRepository.findByRollNumber(rollNumber);
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
