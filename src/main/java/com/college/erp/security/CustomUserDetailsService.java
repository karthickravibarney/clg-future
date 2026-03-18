package com.college.erp.security;

import com.college.erp.model.Staff;
import com.college.erp.model.Student;
import com.college.erp.repository.StaffRepository;
import com.college.erp.repository.StudentRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;

    public CustomUserDetailsService(StudentRepository studentRepository, StaffRepository staffRepository) {
        this.studentRepository = studentRepository;
        this.staffRepository = staffRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Check Hardcoded Admin
        if ("admin".equals(username)) {
            return new User("admin", "{noop}admin123", 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        // 2. Check Student
        Optional<Student> student = studentRepository.findByRollNumber(username);
        if (student.isPresent()) {
            return new User(student.get().getRollNumber(), "{noop}" + student.get().getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT")));
        }

        // 3. Check Staff
        Optional<Staff> staff = staffRepository.findByEmployeeId(username);
        if (staff.isPresent()) {
            String designation = staff.get().getDesignation();
            String role;
            if ("PRINCIPAL".equals(designation) || "VICE_PRINCIPAL".equals(designation)) {
                role = "ROLE_PRINCIPAL";
            } else if ("HOD".equals(designation)) {
                role = "ROLE_HOD";
            } else {
                role = "ROLE_STAFF";
            }
            return new User(staff.get().getEmployeeId(), "{noop}" + staff.get().getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(role)));
        }

        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}
