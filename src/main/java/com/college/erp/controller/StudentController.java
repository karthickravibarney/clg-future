package com.college.erp.controller;

import com.college.erp.model.Student;
import com.college.erp.service.AcademicService;
import com.college.erp.service.StudentService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.college.erp.service.FileStorageService;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final AcademicService academicService;
    private final StudentService studentService;
    private final FileStorageService fileStorageService;

    public StudentController(AcademicService academicService, StudentService studentService,
            FileStorageService fileStorageService) {
        this.academicService = academicService;
        this.studentService = studentService;
        this.fileStorageService = fileStorageService;
    }

    private String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String rollNo = getUsername();
        if (rollNo == null)
            return "redirect:/login";

        Student student = studentService.getStudentByRollNumber(rollNo).orElse(null);
        if (student != null) {
            model.addAttribute("student", student);
            model.addAttribute("attendanceCount", academicService.getAttendanceByStudent(student).size());
            model.addAttribute("marks", academicService.getMarksByStudent(student));
        }
        return "student/dashboard";
    }

    @GetMapping("/timetable")
    public String timetable(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester,
            Model model) {
        String rollNo = getUsername();
        Student student = studentService.getStudentByRollNumber(rollNo).orElse(null);

        if (student != null) {
            if (year == null || semester == null) {
                try {
                    com.college.erp.model.Batch batch = student.getBatch();
                    if (batch != null) {
                        String batchName = batch.getName(); // e.g., "2024-2028"
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

                        year = Math.max(1,
                                Math.min(student.getDepartment() != null ? student.getDepartment().getMaxYears() : 4,
                                        calculatedYear));
                        semester = Math.max(1,
                                Math.min(
                                        student.getDepartment() != null ? student.getDepartment().getMaxSemesters() : 8,
                                        calculatedSem));
                    }
                } catch (Exception e) {
                    // Fallback handled below
                }
            }

            if (year == null)
                year = 1;
            if (semester == null)
                semester = 1;

            model.addAttribute("selectedYear", year);
            model.addAttribute("selectedSemester", semester);

            if (student.getBatch() != null) {
                java.util.List<com.college.erp.model.TimetableEntry> timetable = academicService
                        .getTimetableByBatchYearAndSem(student.getBatch(), year, semester);

                java.util.Map<String, java.util.Map<Integer, com.college.erp.model.TimetableEntry>> timetableMap = new java.util.HashMap<>();
                for (com.college.erp.model.TimetableEntry entry : timetable) {
                    if (entry.getDayOfWeek() != null) {
                        timetableMap.computeIfAbsent(entry.getDayOfWeek().toUpperCase(), k -> new java.util.HashMap<>())
                                .put(entry.getPeriodNumber(), entry);
                    }
                }
                model.addAttribute("timetableMap", timetableMap);
            }

            if (student.getDepartment() != null) {
                java.util.List<com.college.erp.model.PeriodTiming> timingsList = academicService
                        .getPeriodTimings(student.getDepartment());
                java.util.Map<Integer, com.college.erp.model.PeriodTiming> timingsMap = timingsList.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.college.erp.model.PeriodTiming::getPeriodNumber,
                                t -> t,
                                (existing, replacement) -> existing));
                model.addAttribute("timingsMap", timingsMap);
                model.addAttribute("maxYears", Math.max(1, student.getDepartment().getMaxYears()));
                model.addAttribute("maxSemesters", Math.max(1, student.getDepartment().getMaxSemesters()));
            } else {
                model.addAttribute("maxYears", 4);
                model.addAttribute("maxSemesters", 8);
            }
        }
        return "student/timetable";
    }

    @GetMapping("/attendance")
    public String attendance(Model model) {
        String username = getUsername();
        if (username == null)
            return "redirect:/login";
        Student student = studentService.getStudentByRollNumber(username).orElseThrow();

        java.util.List<com.college.erp.model.Attendance> attendances = academicService.getAttendanceByStudent(student);
        java.util.Map<java.time.LocalDate, java.util.Map<Integer, com.college.erp.model.Attendance>> attendanceMap = new java.util.TreeMap<>(
                java.util.Collections.reverseOrder());
        for (com.college.erp.model.Attendance a : attendances) {
            attendanceMap.computeIfAbsent(a.getDate(), k -> new java.util.HashMap<>()).put(a.getPeriodNumber(), a);
        }

        model.addAttribute("attendanceMap", attendanceMap);

        // Add active period for highlighting
        if (student.getDepartment() != null) {
            Integer activePeriod = academicService.getCurrentPeriodNumber(student.getDepartment());
            model.addAttribute("activePeriod", activePeriod);
        }

        return "student/attendance";
    }

    @GetMapping("/results")
    public String results(Model model) {
        String username = getUsername();
        if (username == null)
            return "redirect:/login";
        Student student = studentService.getStudentByRollNumber(username).orElseThrow();
        model.addAttribute("marks", academicService.getMarksByStudent(student));
        return "student/results";
    }

    @GetMapping("/schedule")
    public String schedule(Model model) {
        String username = getUsername();
        if (username == null)
            return "redirect:/login";
        Student student = studentService.getStudentByRollNumber(username).orElseThrow();
        model.addAttribute("exams", academicService.getExamScheduleByBatch(student.getBatch()));
        return "student/schedule";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        String username = getUsername();
        if (username == null)
            return "redirect:/login";
        Student student = studentService.getStudentByRollNumber(username).orElseThrow();
        model.addAttribute("student", student);
        return "student/profile";
    }

    @PostMapping("/profile/upload")
    public String uploadDocuments(
            @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto,
            @RequestParam(value = "aadhaar", required = false) MultipartFile aadhaar,
            @RequestParam(value = "pan", required = false) MultipartFile pan,
            @RequestParam(value = "income", required = false) MultipartFile income,
            @RequestParam(value = "community", required = false) MultipartFile community,
            @RequestParam(value = "bank", required = false) MultipartFile bank,
            @RequestParam(value = "marksheet", required = false) MultipartFile marksheet,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "contactNumber", required = false) String contactNumber,
            @RequestParam(value = "familyContactNumber", required = false) String familyContactNumber,
            @RequestParam(value = "email", required = false) String email,
            RedirectAttributes ra) {
        String username = getUsername();
        if (username == null)
            return "redirect:/login";

        Student student = studentService.getStudentByRollNumber(username).orElseThrow();

        if (email != null && !email.isEmpty())
            student.setEmail(email);
        if (address != null)
            student.setAddress(address);
        if (contactNumber != null)
            student.setContactNumber(contactNumber);
        if (familyContactNumber != null)
            student.setFamilyContactNumber(familyContactNumber);

        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            student.setProfilePhotoPath(fileStorageService.storeFile(profilePhoto));
        }
        if (aadhaar != null && !aadhaar.isEmpty())
            student.setAadhaarCardPath(fileStorageService.storeFile(aadhaar));
        if (pan != null && !pan.isEmpty())
            student.setPanCardPath(fileStorageService.storeFile(pan));
        if (income != null && !income.isEmpty())
            student.setIncomeCertificatePath(fileStorageService.storeFile(income));
        if (community != null && !community.isEmpty())
            student.setCommunityCertificatePath(fileStorageService.storeFile(community));
        if (bank != null && !bank.isEmpty())
            student.setBankPassbookPath(fileStorageService.storeFile(bank));
        if (marksheet != null && !marksheet.isEmpty())
            student.setMarksheetPath(fileStorageService.storeFile(marksheet));

        studentService.updateStudent(student);
        ra.addFlashAttribute("success", "Documents uploaded successfully.");
        return "redirect:/student/profile";
    }

    @GetMapping("/change-password")
    public String showChangePassword() {
        return "student/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String oldPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes ra) {
        String username = getUsername();
        Student student = studentService.getStudentByRollNumber(username).orElse(null);

        if (student == null) {
            return "redirect:/login";
        }

        if (!student.getPassword().equals(oldPassword)) {
            ra.addFlashAttribute("error", "Current password does not match.");
            return "redirect:/student/change-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match.");
            return "redirect:/student/change-password";
        }

        // Validation: 1 upper, 1 special char
        boolean hasUpper = newPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasSpecial = newPassword.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        if (!hasUpper || !hasSpecial) {
            ra.addFlashAttribute("error", "Password must contain at least one uppercase letter and one special character.");
            return "redirect:/student/change-password";
        }

        student.setPassword(newPassword);
        studentService.updateStudent(student);

        ra.addFlashAttribute("success", "Password updated successfully.");
        return "redirect:/student/profile";
    }
}

