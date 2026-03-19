package com.college.erp.controller;

import com.college.erp.model.Student;
import com.college.erp.model.Staff;
import com.college.erp.service.AcademicService;
import com.college.erp.service.StudentService;
import com.college.erp.service.BatchService;
import com.college.erp.repository.StaffRepository;
import com.college.erp.repository.StudentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

@Controller
@RequestMapping("/staff")
public class StaffController {

    private final AcademicService academicService;
    private final StudentService studentService;
    private final BatchService batchService;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final com.college.erp.service.StaffService staffService;
    private final com.college.erp.service.FileStorageService fileStorageService;

    public StaffController(AcademicService academicService, StudentService studentService,
            BatchService batchService, StaffRepository staffRepository,
            StudentRepository studentRepository,
            com.college.erp.service.StaffService staffService,
            com.college.erp.service.FileStorageService fileStorageService) {
        this.academicService = academicService;
        this.studentService = studentService;
        this.batchService = batchService;
        this.staffRepository = staffRepository;
        this.studentRepository = studentRepository;
        this.staffService = staffService;
        this.fileStorageService = fileStorageService;
    }

    private String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }

    private String getRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        }
        return null;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String empId = getUsername();
        if (empId == null)
            return "redirect:/login";

        Staff staff = staffRepository.findByEmployeeId(empId).orElse(null);
        if (staff != null) {
            String role = getRole();
            model.addAttribute("staff", staff);
            if ("HOD".equals(role)) {
                // HOD sees department specific counts
                model.addAttribute("deptCount", 1);
                model.addAttribute("studentCount", studentRepository.countByDepartment(staff.getDepartment()));
                model.addAttribute("staffCount", staffRepository.countByDepartment(staff.getDepartment()));
            } else {
                // Other staff see overall counts or simplified view
                model.addAttribute("deptCount", 1);
                model.addAttribute("studentCount", studentRepository.count());
                model.addAttribute("staffCount", staffRepository.count());
            }
        }

        model.addAttribute("empId", empId);
        return "staff/dashboard";
    }

    private List<Student> getStudentsForStaff() {
        String role = getRole();
        if ("ADMIN".equals(role) || "PRINCIPAL".equals(role)) {
            return studentService.getAllStudents();
        }

        String username = getUsername();
        Staff currentStaff = staffRepository.findByEmployeeId(username).orElse(null);
        if (currentStaff != null) {
            return studentService.getAllStudents().stream()
                    .filter(s -> s.getDepartment().getId().equals(currentStaff.getDepartment().getId()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @GetMapping("/attendance")
    public String attendance(@RequestParam(required = false) Integer period,
            @RequestParam(required = false) Long batchId,
            Model model) {
        String username = getUsername();
        Staff currentStaff = staffRepository.findByEmployeeId(username).orElse(null);

        if (currentStaff == null) {
            return "redirect:/login";
        }

        Integer activePeriod = academicService.getCurrentPeriodNumber(currentStaff.getDepartment());
        if (period == null) {
            period = (activePeriod != null) ? activePeriod : 1;
        }

        List<com.college.erp.model.Batch> batches = batchService.getAllBatches().stream()
                .filter(b -> b.getDepartment().getId().equals(currentStaff.getDepartment().getId()))
                .collect(Collectors.toList());
        
        // Dynamic max periods based on configuration
        List<com.college.erp.model.PeriodTiming> timings = academicService.getPeriodTimings(currentStaff.getDepartment());
        int maxPeriods = timings.stream()
                .mapToInt(com.college.erp.model.PeriodTiming::getPeriodNumber)
                .max().orElse(8);
        if (maxPeriods < 8) maxPeriods = 8; // Minimum 8 buttons for UI consistency

        model.addAttribute("batches", batches);
        model.addAttribute("selectedBatchId", batchId);
        model.addAttribute("currentPeriod", period);
        model.addAttribute("activePeriod", activePeriod);
        model.addAttribute("maxPeriods", maxPeriods);

        List<Student> students = Collections.emptyList();
        if (batchId != null) {
            students = studentService.getAllStudents().stream()
                    .filter(s -> s.getBatch().getId().equals(batchId))
                    .collect(Collectors.toList());
        }
        model.addAttribute("students", students);

        boolean isHod = "HOD".equalsIgnoreCase(currentStaff.getDesignation());
        boolean canMarkAttendance = isHod;

        if (!isHod && batchId != null) {
            com.college.erp.model.Batch selectedBatch = batches.stream()
                    .filter(b -> b.getId().equals(batchId))
                    .findFirst().orElse(null);
            if (selectedBatch != null) {
                canMarkAttendance = academicService.hasAttendancePermission(currentStaff, selectedBatch, period);
            }
        }

        model.addAttribute("isHod", isHod);
        model.addAttribute("canMarkAttendance", canMarkAttendance);

        Map<Long, com.college.erp.model.Attendance> attendanceMap = new HashMap<>();
        for (Student student : students) {
            List<com.college.erp.model.Attendance> attendances = academicService
                    .getAttendanceByStudentAndDate(student, java.time.LocalDate.now());
            for (com.college.erp.model.Attendance att : attendances) {
                if (att.getPeriodNumber() == period) {
                    attendanceMap.put(student.getId(), att);
                    break;
                }
            }
        }
        model.addAttribute("attendanceMap", attendanceMap);

        return "staff/attendance";
    }

    @GetMapping("/marks")
    public String marks(Model model) {
        model.addAttribute("students", getStudentsForStaff());
        return "staff/marks";
    }

    @GetMapping("/student/{id}")
    public String viewStudentProfile(@PathVariable Long id, Model model) {
        Student student = getStudentsForStaff().stream()
                .filter(s -> s.getId().equals(id))
                .findFirst().orElse(null);
        if (student == null) {
            return "redirect:/staff/attendance";
        }
        model.addAttribute("student", student);
        return "student-profile-view";
    }

    @GetMapping("/profile")
    public String viewMyProfile(Model model) {
        String username = getUsername();
        if (username == null)
            return "redirect:/login";

        Staff staff = staffService.getAllStaff().stream()
                .filter(s -> s.getEmployeeId().equals(username))
                .findFirst().orElseThrow();
        model.addAttribute("staff", staff);
        return "staff/profile";
    }

    @PostMapping("/profile/upload")
    public String uploadProfilePhoto(
            @RequestParam(value = "profilePhoto", required = false) org.springframework.web.multipart.MultipartFile profilePhoto,
            @RequestParam(value = "aadhaar", required = false) org.springframework.web.multipart.MultipartFile aadhaar,
            @RequestParam(value = "pan", required = false) org.springframework.web.multipart.MultipartFile pan,
            @RequestParam(value = "bank", required = false) org.springframework.web.multipart.MultipartFile bank,
            @RequestParam(value = "degree", required = false) org.springframework.web.multipart.MultipartFile degree,
            @RequestParam(value = "marksheet", required = false) org.springframework.web.multipart.MultipartFile marksheet,
            @RequestParam(value = "uan", required = false) org.springframework.web.multipart.MultipartFile uan,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "contactNumber", required = false) String contactNumber,
            @RequestParam(value = "familyContactNumber", required = false) String familyContactNumber,
            @RequestParam(value = "email", required = false) String email,
            RedirectAttributes ra) {
        String username = getUsername();
        if (username == null)
            return "redirect:/login";

        Staff staff = staffRepository.findByEmployeeId(username).orElseThrow();

        if (email != null && !email.isEmpty())
            staff.setEmail(email);
        if (address != null)
            staff.setAddress(address);
        if (contactNumber != null)
            staff.setContactNumber(contactNumber);
        if (familyContactNumber != null)
            staff.setFamilyContactNumber(familyContactNumber);

        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            staff.setProfilePhotoPath(fileStorageService.storeFile(profilePhoto));
        }
        if (aadhaar != null && !aadhaar.isEmpty())
            staff.setAadhaarCardPath(fileStorageService.storeFile(aadhaar));
        if (pan != null && !pan.isEmpty())
            staff.setPanCardPath(fileStorageService.storeFile(pan));
        if (bank != null && !bank.isEmpty())
            staff.setBankPassbookPath(fileStorageService.storeFile(bank));
        if (degree != null && !degree.isEmpty())
            staff.setDegreeCertificatePath(fileStorageService.storeFile(degree));
        if (marksheet != null && !marksheet.isEmpty())
            staff.setMarksheetPath(fileStorageService.storeFile(marksheet));
        if (uan != null && !uan.isEmpty())
            staff.setUanDocumentPath(fileStorageService.storeFile(uan));

        staffRepository.save(staff);
        ra.addFlashAttribute("success", "Documents uploaded successfully.");
        return "redirect:/staff/profile";
    }

    @PostMapping("/attendance/mark")
    public String markAttendance(@RequestParam Long studentId, @RequestParam boolean isPresent,
            @RequestParam(defaultValue = "1") int period,
            @RequestParam(required = false) Long batchId,
            RedirectAttributes ra) {
        Student student = studentService.getAllStudents().stream()
                .filter(s -> s.getId().equals(studentId))
                .findFirst().orElse(null);

        String username = getUsername();
        Staff staff = staffRepository.findByEmployeeId(username).orElse(null);

        com.college.erp.model.Batch batch = null;
        if (batchId != null) {
            batch = batchService.getAllBatches().stream().filter(b -> b.getId().equals(batchId)).findFirst().orElse(null);
        }

        if (batch != null && !academicService.hasAttendancePermission(staff, batch, period)) {
            ra.addFlashAttribute("error", "You do not have permission to mark attendance for this period.");
        } else {
            academicService.markAttendance(student, isPresent, staff, period);
            ra.addFlashAttribute("success", "Attendance marked for " + student.getFullName() + " (Period " + period + ")");
        }

        ra.addAttribute("period", period);
        if (batchId != null) {
            ra.addAttribute("batchId", batchId);
        }
        return "redirect:/staff/attendance";
    }

    @PostMapping("/marks/save")
    public String saveMarks(@RequestParam String subject, @RequestParam int semester,
            @RequestParam double totalMarks, @RequestParam Map<String, String> params,
            RedirectAttributes ra) {
        for (String key : params.keySet()) {
            if (key.startsWith("marks[") && key.endsWith("].score")) {
                int index = Integer.parseInt(key.substring(6, key.indexOf("]")));
                Long studentId = Long.parseLong(params.get("marks[" + index + "].studentId"));
                double score = Double.parseDouble(params.get(key));

                Student student = studentService.getAllStudents().stream()
                        .filter(s -> s.getId().equals(studentId))
                        .findFirst().orElse(null);

                academicService.enterMarks(com.college.erp.model.Marks.builder()
                        .student(student)
                        .subject(subject)
                        .semester(semester)
                        .marksObtained(score)
                        .totalMarks(totalMarks)
                        .build());
            }
        }
        ra.addFlashAttribute("success", "Marks saved successfully for " + subject);
        return "redirect:/staff/marks";
    }

    @GetMapping("/timetable")
    public String timetable(@RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester,
            Model model) {
        String username = getUsername();
        Staff currentStaff = staffRepository.findByEmployeeId(username).orElse(null);

        if (currentStaff == null) {
            return "redirect:/login";
        }

        List<com.college.erp.model.Batch> batches = batchService.getAllBatches().stream()
                .filter(b -> b.getDepartment().getId().equals(currentStaff.getDepartment().getId()))
                .collect(Collectors.toList());
        model.addAttribute("batches", batches);
        
        Integer activePeriod = academicService.getCurrentPeriodNumber(currentStaff.getDepartment());
        model.addAttribute("activePeriod", activePeriod);

        model.addAttribute("maxYears", 4);
        model.addAttribute("maxSemesters", 8);

        com.college.erp.model.Batch batch = null;
        if (batchId != null) {
            final Long finalBatchId = batchId;
            batch = batches.stream()
                    .filter(b -> b.getId().equals(finalBatchId))
                    .findFirst().orElse(null);

            if (batch != null) {
                if (year == null || semester == null) {
                    try {
                        String batchName = batch.getName();
                        int startYear = Integer.parseInt(batchName.split("-")[0]);
                        int currentYear = java.time.LocalDate.now().getYear();
                        int currentMonth = java.time.LocalDate.now().getMonthValue();

                        int calculatedYear = currentYear - startYear + 1;
                        int calculatedSem;
                        if (currentMonth >= 7) {
                            calculatedSem = (calculatedYear * 2) - 1;
                        } else {
                            calculatedSem = (calculatedYear * 2);
                            calculatedYear = calculatedYear - 1;
                        }

                        if (year == null)
                            year = Math.max(1, Math.min(batch.getDepartment().getMaxYears(), calculatedYear));
                        if (semester == null)
                            semester = Math.max(1, Math.min(batch.getDepartment().getMaxSemesters(), calculatedSem));
                    } catch (Exception e) {
                    }
                }

                if (year == null)
                    year = 1;
                if (semester == null)
                    semester = 1;

                model.addAttribute("selectedBatch", batch);
                model.addAttribute("selectedYear", year);
                model.addAttribute("selectedSemester", semester);

                List<com.college.erp.model.TimetableEntry> entries = academicService.getTimetableByBatchYearAndSem(
                        batch, year, semester);
                Map<String, Map<Integer, com.college.erp.model.TimetableEntry>> timetableMap = new HashMap<>();
                for (com.college.erp.model.TimetableEntry entry : entries) {
                    if (entry.getDayOfWeek() != null) {
                        timetableMap.computeIfAbsent(entry.getDayOfWeek().toUpperCase(), k -> new HashMap<>())
                                .put(entry.getPeriodNumber(), entry);
                    }
                }
                model.addAttribute("timetableMap", timetableMap);

                java.util.List<com.college.erp.model.PeriodTiming> timingsList = academicService
                        .getPeriodTimings(batch.getDepartment());
                java.util.Map<Integer, com.college.erp.model.PeriodTiming> timingsMap = timingsList.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.college.erp.model.PeriodTiming::getPeriodNumber,
                                t -> t,
                                (existing, replacement) -> existing));
                model.addAttribute("timingsMap", timingsMap);
            }
        }

        // Ensure defaults are present even if no batch is selected
        if (year == null)
            year = 1;
        if (semester == null)
            semester = 1;
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("currentStaffId", currentStaff.getId());

        if (batch != null) {
            model.addAttribute("maxYears", Math.max(1, batch.getDepartment().getMaxYears()));
            model.addAttribute("maxSemesters", Math.max(1, batch.getDepartment().getMaxSemesters()));
        }

        return "staff/timetable";
    }

    @GetMapping("/change-password")
    public String showChangePassword() {
        return "staff/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes ra) {
        String username = getUsername();
        Staff staff = staffRepository.findByEmployeeId(username).orElse(null);

        if (staff == null) {
            return "redirect:/login";
        }

        if (!staff.getPassword().equals(oldPassword)) {
            ra.addFlashAttribute("error", "Current password does not match.");
            return "redirect:/staff/change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match.");
            return "redirect:/staff/change-password";
        }

        // Validation: 1 upper, 1 special char
        boolean hasUpper = newPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasSpecial = newPassword.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        if (!hasUpper || !hasSpecial) {
            ra.addFlashAttribute("error", "Password must contain at least one uppercase letter and one special character.");
            return "redirect:/staff/change-password";
        }

        staff.setPassword(newPassword);
        staffRepository.save(staff);

        ra.addFlashAttribute("success", "Password updated successfully.");
        return "redirect:/staff/profile";
    }
}

