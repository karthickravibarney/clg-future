package com.college.erp.service;

import com.college.erp.model.Staff;
import com.college.erp.repository.StaffRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Random;

@Service
public class StaffService {
    private final StaffRepository staffRepository;
    private final com.college.erp.repository.TimetableEntryRepository timetableEntryRepository;
    private final com.college.erp.repository.AttendanceRepository attendanceRepository;

    public StaffService(StaffRepository staffRepository,
            com.college.erp.repository.TimetableEntryRepository timetableEntryRepository,
            com.college.erp.repository.AttendanceRepository attendanceRepository) {
        this.staffRepository = staffRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    public Staff createStaff(Staff staff) {
        // Robust Employee ID generation: Find max ID and increment
        List<Staff> allStaff = staffRepository.findAll();
        int maxId = 0;
        for (Staff s : allStaff) {
            String empId = s.getEmployeeId();
            if (empId != null && empId.startsWith("EMP")) {
                try {
                    int idNum = Integer.parseInt(empId.substring(3));
                    if (idNum > maxId) {
                        maxId = idNum;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        staff.setEmployeeId(String.format("EMP%03d", maxId + 1));

        String generatedPassword = generateRandomPassword(8);
        staff.setPassword(generatedPassword);
        staff.setTempPassword(generatedPassword);

        return staffRepository.save(staff);
    }

    public Staff updateStaff(Long id, Staff updatedStaff) {
        Staff existingStaff = staffRepository.findById(id).orElseThrow(() -> new RuntimeException("Staff not found"));
        existingStaff.setFullName(updatedStaff.getFullName());
        existingStaff.setEmail(updatedStaff.getEmail());
        existingStaff.setDesignation(updatedStaff.getDesignation());
        existingStaff.setDepartment(updatedStaff.getDepartment());
        // Do not update password or employee ID here
        return staffRepository.save(existingStaff);
    }

    public void deleteStaff(Long id) {
        Staff staff = staffRepository.findById(id).orElse(null);
        if (staff != null) {
            List<com.college.erp.model.TimetableEntry> entries = timetableEntryRepository.findByStaff(staff);
            for (com.college.erp.model.TimetableEntry entry : entries) {
                entry.setStaff(null);
                timetableEntryRepository.save(entry);
            }

            List<com.college.erp.model.Attendance> attendances = attendanceRepository.findByMarkedBy(staff);
            for (com.college.erp.model.Attendance attendance : attendances) {
                attendance.setMarkedBy(null);
                attendanceRepository.save(attendance);
            }

            staffRepository.deleteById(id);
        }
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
