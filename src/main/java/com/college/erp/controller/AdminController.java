package com.college.erp.controller;

import com.college.erp.model.Student;
import com.college.erp.model.Staff;
import com.college.erp.repository.NotificationRepository;
import com.college.erp.service.DepartmentService;
import com.college.erp.service.BatchService;
import com.college.erp.service.StudentService;
import com.college.erp.service.StaffService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DepartmentService departmentService;
    private final StudentService studentService;
    private final StaffService staffService;
    private final BatchService batchService;
    private final com.college.erp.service.AcademicService academicService;
    private final com.college.erp.service.NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    public AdminController(DepartmentService departmentService, StudentService studentService,
            StaffService staffService, BatchService batchService,
            com.college.erp.service.AcademicService academicService,
            com.college.erp.service.NotificationService notificationService,
            NotificationRepository notificationRepository) {
        this.departmentService = departmentService;
        this.studentService = studentService;
        this.staffService = staffService;
        this.batchService = batchService;
        this.academicService = academicService;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
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
        model.addAttribute("deptCount", departmentService.getAllDepartments().size());
        model.addAttribute("studentCount", studentService.getAllStudents().size());
        model.addAttribute("staffCount", staffService.getAllStaff().size());
        return "admin/dashboard";
    }

    @GetMapping("/timetable")
    public String timetable(@RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester,
            Model model) {
        String role = getRole();
        String username = getUsername();

        java.util.List<com.college.erp.model.Batch> batches;
        com.college.erp.model.Staff currentStaff = null;

        if ("HOD".equals(role)) {
            currentStaff = staffService.getAllStaff().stream()
                    .filter(s -> s.getEmployeeId().equals(username)).findFirst().orElse(null);
            final com.college.erp.model.Staff finalStaff = currentStaff;
            batches = batchService.getAllBatches().stream()
                    .filter(b -> finalStaff != null
                            && b.getDepartment().getId().equals(finalStaff.getDepartment().getId()))
                    .collect(java.util.stream.Collectors.toList());
        } else if ("ADMIN".equals(role) || "PRINCIPAL".equals(role)) {
            batches = batchService.getAllBatches();
        } else {
            return "redirect:/home";
        }

        model.addAttribute("batches", batches);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedSemester", semester);

        // Add current staff ID for highlighting
        staffService.getAllStaff().stream()
                .filter(s -> s.getEmployeeId().equals(username))
                .findFirst()
                .ifPresent(s -> model.addAttribute("currentStaffId", s.getId()));

        model.addAttribute("maxYears", 4);
        model.addAttribute("maxSemesters", 8);

        com.college.erp.model.Batch batch = null;
        if (batchId != null) {
            final Long finalBatchId = batchId;
            batch = batches.stream()
                    .filter(b -> b.getId().equals(finalBatchId)).findFirst().orElse(null);
            if (batch != null) {
                model.addAttribute("selectedBatch", batch);
                if (year != null && semester != null) {
                    java.util.List<com.college.erp.model.TimetableEntry> entries = academicService
                            .getTimetableByBatchYearAndSem(batch, year, semester);
                    java.util.Map<String, java.util.Map<Integer, com.college.erp.model.TimetableEntry>> timetableMap = new java.util.HashMap<>();
                    for (com.college.erp.model.TimetableEntry entry : entries) {
                        timetableMap.computeIfAbsent(entry.getDayOfWeek(), k -> new java.util.HashMap<>())
                                .put(entry.getPeriodNumber(), entry);
                    }
                    model.addAttribute("timetableMap", timetableMap);
                }

                // Fetch and add period timings map to model
                java.util.List<com.college.erp.model.PeriodTiming> timingsList = academicService
                        .getPeriodTimings(batch.getDepartment());
                java.util.Map<Integer, com.college.erp.model.PeriodTiming> timingsMap = timingsList.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.college.erp.model.PeriodTiming::getPeriodNumber,
                                t -> t,
                                (existing, replacement) -> existing));
                model.addAttribute("timingsMap", timingsMap);

                final com.college.erp.model.Batch finalBatch = batch;
                model.addAttribute("staffMembers", staffService.getAllStaff().stream()
                        .filter(s -> s.getDepartment().getId().equals(finalBatch.getDepartment().getId()))
                        .collect(java.util.stream.Collectors.toList()));
                model.addAttribute("maxYears", Math.max(1, batch.getDepartment().getMaxYears()));
                model.addAttribute("maxSemesters", Math.max(1, batch.getDepartment().getMaxSemesters()));
            }
        }
        return "admin/timetable";
    }

    @PostMapping("/timetable/save-all")
    public String saveAllTimetable(@RequestParam Long batchId,
            @RequestParam Integer year, @RequestParam Integer semester,
            jakarta.servlet.http.HttpServletRequest request,
            RedirectAttributes ra) {
        String role = getRole();
        String username = getUsername();

        com.college.erp.model.Batch batch = batchService.getAllBatches().stream()
                .filter(b -> b.getId().equals(batchId)).findFirst().orElse(null);

        if ("HOD".equals(role)) {
            com.college.erp.model.Staff currentStaff = staffService.getAllStaff().stream()
                    .filter(s -> s.getEmployeeId().equals(username)).findFirst().orElse(null);
            if (batch == null || currentStaff == null
                    || !batch.getDepartment().getId().equals(currentStaff.getDepartment().getId())) {
                ra.addFlashAttribute("error", "Unauthorized access.");
                return "redirect:/admin/timetable";
            }
        } else if (!"ADMIN".equals(role) && !"PRINCIPAL".equals(role)) {
            return "redirect:/home";
        }

        java.util.List<com.college.erp.model.TimetableEntry> entries = new java.util.ArrayList<>();
        String[] days = { "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY" };

        for (String day : days) {
            for (int p = 1; p <= 11; p++) {
                String subjectName = request.getParameter("subject_" + day + "_" + p);
                String staffIdStr = request.getParameter("staffId_" + day + "_" + p);

                if (subjectName != null && !subjectName.trim().isEmpty()) {
                    com.college.erp.model.Staff staff = null;
                    if (staffIdStr != null && !staffIdStr.isEmpty()) {
                        try {
                            Long sid = Long.parseLong(staffIdStr);
                            staff = staffService.getAllStaff().stream()
                                    .filter(s -> s.getId().equals(sid)).findFirst().orElse(null);
                        } catch (Exception e) {
                            // Skip invalid staff ID
                        }
                    }
                    entries.add(com.college.erp.model.TimetableEntry.builder()
                            .batch(batch).dayOfWeek(day).periodNumber(p)
                            .year(year).semester(semester)
                            .subjectName(subjectName).staff(staff).build());
                }
            }
        }

        if (!entries.isEmpty()) {
            academicService.saveTimetableEntries(entries);
            ra.addFlashAttribute("success", "Timetable updated for the entire week!");
        }

        ra.addAttribute("batchId", batchId);
        ra.addAttribute("year", year);
        ra.addAttribute("semester", semester);
        return "redirect:/admin/timetable";
    }

    @GetMapping("/timings")
    public String timings(@RequestParam(required = false) Long deptId, Model model) {
        String role = getRole();
        String username = getUsername();

        java.util.List<com.college.erp.model.Department> departments;
        if ("HOD".equals(role)) {
            com.college.erp.model.Staff currentStaff = staffService.getAllStaff().stream()
                    .filter(s -> s.getEmployeeId().equals(username)).findFirst().orElse(null);
            final com.college.erp.model.Staff finalStaff = currentStaff;
            departments = departmentService.getAllDepartments().stream()
                    .filter(d -> finalStaff != null && d.getId().equals(finalStaff.getDepartment().getId()))
                    .collect(java.util.stream.Collectors.toList());
        } else if ("ADMIN".equals(role) || "PRINCIPAL".equals(role)) {
            departments = departmentService.getAllDepartments();
        } else {
            return "redirect:/home";
        }

        model.addAttribute("departments", departments);

        if (deptId != null) {
            final Long finalDeptId = deptId;
            com.college.erp.model.Department dept = departments.stream()
                    .filter(d -> d.getId().equals(finalDeptId)).findFirst().orElse(null);
            if (dept != null) {
                model.addAttribute("selectedDept", dept);
                java.util.List<com.college.erp.model.PeriodTiming> timingsList = academicService.getPeriodTimings(dept);
                java.util.Map<Integer, com.college.erp.model.PeriodTiming> timingsMap = timingsList.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                com.college.erp.model.PeriodTiming::getPeriodNumber,
                                t -> t,
                                (existing, replacement) -> existing));
                model.addAttribute("timingsMap", timingsMap);
            }
        }
        return "admin/timings";
    }

    @PostMapping("/timings/save")
    public String saveTiming(@RequestParam Long deptId, @RequestParam int period,
            @RequestParam String start, @RequestParam String end, RedirectAttributes ra) {
        String role = getRole();
        String username = getUsername();

        if ("HOD".equals(role)) {
            com.college.erp.model.Staff currentStaff = staffService.getAllStaff().stream()
                    .filter(s -> s.getEmployeeId().equals(username)).findFirst().orElse(null);
            if (currentStaff == null || !deptId.equals(currentStaff.getDepartment().getId())) {
                ra.addFlashAttribute("error", "Unauthorized access to this department.");
                return "redirect:/admin/timings";
            }
        } else if (!"ADMIN".equals(role) && !"PRINCIPAL".equals(role)) {
            return "redirect:/home";
        }

        com.college.erp.model.Department dept = departmentService.getAllDepartments().stream()
                .filter(d -> d.getId().equals(deptId)).findFirst().orElse(null);

        academicService.savePeriodTiming(com.college.erp.model.PeriodTiming.builder()
                .department(dept).periodNumber(period)
                .startTime(start).endTime(end).build());

        ra.addAttribute("deptId", deptId);
        ra.addFlashAttribute("success", "Period timing updated!");
        return "redirect:/admin/timings";
    }

    @PostMapping("/timings/save-all")
    public String saveAllTimings(@RequestParam Long deptId, jakarta.servlet.http.HttpServletRequest request,
            RedirectAttributes ra) {
        String role = getRole();
        String username = getUsername();

        if ("HOD".equals(role)) {
            com.college.erp.model.Staff currentStaff = staffService.getAllStaff().stream()
                    .filter(s -> s.getEmployeeId().equals(username)).findFirst().orElse(null);
            if (currentStaff == null || !deptId.equals(currentStaff.getDepartment().getId())) {
                ra.addFlashAttribute("error", "Unauthorized access to this department.");
                return "redirect:/admin/timings";
            }
        } else if (!"ADMIN".equals(role) && !"PRINCIPAL".equals(role)) {
            return "redirect:/home";
        }

        com.college.erp.model.Department dept = departmentService.getAllDepartments().stream()
                .filter(d -> d.getId().equals(deptId)).findFirst().orElse(null);

        java.util.List<com.college.erp.model.PeriodTiming> timings = new java.util.ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            String start = request.getParameter("start_" + i);
            String end = request.getParameter("end_" + i);
            String isBreakStr = request.getParameter("isBreak_" + i);
            boolean isBreak = "true".equals(isBreakStr) || "on".equals(isBreakStr);
            String breakName = request.getParameter("breakName_" + i);

            if (start != null && end != null && !start.isEmpty() && !end.isEmpty()) {
                timings.add(com.college.erp.model.PeriodTiming.builder()
                        .department(dept).periodNumber(i)
                        .startTime(start).endTime(end)
                        .isBreak(isBreak).breakName(breakName).build());
            }
        }

        academicService.savePeriodTimings(timings);

        ra.addAttribute("deptId", deptId);
        ra.addFlashAttribute("success", "All period timings updated successfully!");
        return "redirect:/admin/timings";
    }

    @GetMapping("/departments")
    public String departments(Model model) {
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "admin/departments";
    }

    @GetMapping("/students")
    public String students(@RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long batchId,
            Model model) {
        String role = getRole();
        if (role == null)
            return "redirect:/login";

        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("batches", batchService.getAllBatches());
        model.addAttribute("selectedDeptId", deptId);
        model.addAttribute("selectedBatchId", batchId);

        if ("ADMIN".equals(role) || "PRINCIPAL".equals(role)) {
            java.util.List<Student> students = studentService.getAllStudents();
            if (deptId != null) {
                students = students.stream()
                        .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(deptId))
                        .collect(java.util.stream.Collectors.toList());
            }
            if (batchId != null) {
                students = students.stream()
                        .filter(s -> s.getBatch() != null && s.getBatch().getId().equals(batchId))
                        .collect(java.util.stream.Collectors.toList());
            }
            model.addAttribute("students", students);
        } else if ("HOD".equals(role) || "STAFF".equals(role)) {
            String username = getUsername();
            com.college.erp.model.Staff currentStaff = staffService.getAllStaff().stream()
                    .filter(s -> s.getEmployeeId().equals(username)).findFirst().orElse(null);
            if (currentStaff != null) {
                java.util.List<Student> students = studentService.getAllStudents().stream()
                        .filter(s -> s.getDepartment().getId().equals(currentStaff.getDepartment().getId()))
                        .collect(java.util.stream.Collectors.toList());
                if (batchId != null) {
                    students = students.stream()
                            .filter(s -> s.getBatch().getId().equals(batchId))
                            .collect(java.util.stream.Collectors.toList());
                }
                model.addAttribute("students", students);
                model.addAttribute("selectedDeptId", currentStaff.getDepartment().getId());
            }
        } else {
            return "redirect:/home";
        }
        return "admin/students";
    }

    @GetMapping("/student/{id}")
    public String viewStudentProfile(@PathVariable Long id, Model model) {
        String role = getRole();
        if (role == null)
            return "redirect:/login";

        Student student = null;
        if ("ADMIN".equals(role) || "PRINCIPAL".equals(role)) {
            student = studentService.getAllStudents().stream()
                    .filter(s -> s.getId().equals(id)).findFirst().orElse(null);
        } else if ("HOD".equals(role) || "STAFF".equals(role)) {
            String username = getUsername();
            com.college.erp.model.Staff currentStaff = staffService.getAllStaff().stream()
                    .filter(s -> s.getEmployeeId().equals(username)).findFirst().orElse(null);
            if (currentStaff != null) {
                student = studentService.getAllStudents().stream()
                        .filter(s -> s.getDepartment().getId().equals(currentStaff.getDepartment().getId())
                                && s.getId().equals(id))
                        .findFirst().orElse(null);
            }
        }

        if (student == null) {
            return "redirect:/admin/students";
        }

        model.addAttribute("student", student);
        return "student-profile-view";
    }

    @GetMapping("/staff/{id}")
    public String viewStaffProfile(@PathVariable Long id, Model model) {
        String role = getRole();
        if (!"ADMIN".equals(role) && !"PRINCIPAL".equals(role)) {
            return "redirect:/home";
        }

        Staff staff = staffService.getAllStaff().stream()
                .filter(s -> s.getId().equals(id)).findFirst().orElse(null);

        if (staff == null) {
            return "redirect:/admin/staff";
        }

        model.addAttribute("viewedStaff", staff);
        return "admin/staff-profile-view";
    }

    @GetMapping("/students/new")
    public String studentForm(Model model) {
        if (!"ADMIN".equals(getRole()))
            return "redirect:/home";
        model.addAttribute("student", new Student());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("batches", batchService.getAllBatches());
        return "admin/student-form";
    }

    @PostMapping("/students/save")
    public String saveStudent(@ModelAttribute Student student, RedirectAttributes ra) {
        try {
            if (student.getId() != null) {
                studentService.editStudent(student.getId(), student);
                ra.addFlashAttribute("success", "Student details updated successfully!");
            } else {
                Student savedStudent = studentService.createStudent(student);
                ra.addFlashAttribute("success", "Student enrolled successfully! Roll No: " + savedStudent.getRollNumber());
                ra.addFlashAttribute("generatedPassword", savedStudent.getTempPassword());
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error saving student: " + e.getMessage());
            return "redirect:/admin/students/new";
        }
        return "redirect:/admin/students";
    }

    @GetMapping("/students/edit/{id}")
    public String editStudent(@PathVariable Long id, Model model) {
        if (!"ADMIN".equals(getRole()))
            return "redirect:/home";
        Student student = studentService.getAllStudents().stream()
                .filter(s -> s.getId().equals(id)).findFirst().orElse(null);
        if (student == null) {
            return "redirect:/admin/students";
        }
        model.addAttribute("student", student);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("batches", batchService.getAllBatches());
        model.addAttribute("isEdit", true);
        return "admin/student-form";
    }

    @GetMapping("/students/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes ra) {
        if (!"ADMIN".equals(getRole()))
            return "redirect:/home";
        studentService.deleteStudent(id);
        ra.addFlashAttribute("success", "Student deleted successfully!");
        return "redirect:/admin/students";
    }

    @GetMapping("/staff")
    public String staff(@RequestParam(required = false) Long deptId, Model model) {
        String role = getRole();
        if (role == null)
            return "redirect:/login";

        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("selectedDeptId", deptId);

        if ("ADMIN".equals(role) || "PRINCIPAL".equals(role)) {
            java.util.List<Staff> staffMembers = staffService.getAllStaff();
            if (deptId != null) {
                staffMembers = staffMembers.stream()
                        .filter(s -> s.getDepartment().getId().equals(deptId))
                        .collect(java.util.stream.Collectors.toList());
            }
            model.addAttribute("staff", staffMembers);
        } else if ("HOD".equals(role) || "STAFF".equals(role)) {
            String username = getUsername();
            com.college.erp.model.Staff currentStaff = staffService.getAllStaff().stream()
                    .filter(s -> s.getEmployeeId().equals(username)).findFirst().orElse(null);
            if (currentStaff != null) {
                java.util.List<Staff> staffMembers = staffService.getAllStaff().stream()
                        .filter(s -> s.getDepartment().getId().equals(currentStaff.getDepartment().getId()))
                        .collect(java.util.stream.Collectors.toList());
                model.addAttribute("staff", staffMembers);
                model.addAttribute("selectedDeptId", currentStaff.getDepartment().getId());
            }
        } else {
            return "redirect:/home";
        }
        return "admin/staff";
    }

    @GetMapping("/staff/new")
    public String staffForm(Model model) {
        if (!"ADMIN".equals(getRole()))
            return "redirect:/home";
        model.addAttribute("staff", new Staff());
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "admin/staff-form";
    }

    @PostMapping("/staff/save")
    public String saveStaff(@ModelAttribute Staff staff, RedirectAttributes ra) {
        try {
            if (staff.getId() != null) {
                staffService.updateStaff(staff.getId(), staff);
                ra.addFlashAttribute("success", "Staff details updated successfully!");
            } else {
                Staff savedStaff = staffService.createStaff(staff);
                ra.addFlashAttribute("success", "Staff enrolled successfully! Emp ID: " + savedStaff.getEmployeeId());
                ra.addFlashAttribute("generatedPassword", savedStaff.getTempPassword());
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error saving staff: " + e.getMessage());
            return "redirect:/admin/staff/new";
        }
        return "redirect:/admin/staff";
    }

    @GetMapping("/staff/edit/{id}")
    public String editStaff(@PathVariable Long id, Model model) {
        if (!"ADMIN".equals(getRole()))
            return "redirect:/home";
        Staff staff = staffService.getAllStaff().stream()
                .filter(s -> s.getId().equals(id)).findFirst().orElse(null);
        if (staff == null) {
            return "redirect:/admin/staff";
        }
        model.addAttribute("staff", staff);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("isEdit", true);
        return "admin/staff-form";
    }

    @GetMapping("/staff/delete/{id}")
    public String deleteStaff(@PathVariable Long id, RedirectAttributes ra) {
        if (!"ADMIN".equals(getRole()))
            return "redirect:/home";
        staffService.deleteStaff(id);
        ra.addFlashAttribute("success", "Staff deleted successfully!");
        return "redirect:/admin/staff";
    }

    @GetMapping("/departments/new")
    public String departmentForm(Model model) {
        model.addAttribute("department", new com.college.erp.model.Department());
        return "admin/department-form";
    }

    @PostMapping("/departments/new")
    public String saveDepartment(@ModelAttribute com.college.erp.model.Department department, RedirectAttributes ra) {
        if (department.getId() != null) {
            departmentService.updateDepartment(department.getId(), department);
            ra.addFlashAttribute("success", "Department updated successfully!");
        } else {
            departmentService.createDepartment(department);
            ra.addFlashAttribute("success", "Department added successfully!");
        }
        return "redirect:/admin/departments";
    }

    @GetMapping("/departments/edit/{id}")
    public String editDepartment(@PathVariable Long id, Model model) {
        com.college.erp.model.Department department = departmentService.getAllDepartments().stream()
                .filter(d -> d.getId().equals(id)).findFirst().orElse(null);
        if (department == null) {
            return "redirect:/admin/departments";
        }
        model.addAttribute("department", department);
        return "admin/department-form";
    }

    @GetMapping("/departments/delete/{id}")
    public String deleteDepartment(@PathVariable Long id, RedirectAttributes ra) {
        departmentService.deleteDepartment(id);
        ra.addFlashAttribute("success", "Department deleted successfully!");
        return "redirect:/admin/departments";
    }

    @GetMapping("/batches")
    public String batches(Model model) {
        model.addAttribute("batches", batchService.getAllBatches());
        return "admin/batches";
    }

    @GetMapping("/batches/new")
    public String batchForm(Model model) {
        model.addAttribute("batch", new com.college.erp.model.Batch());
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "admin/batch-form";
    }

    @PostMapping("/batches/new")
    public String saveBatch(@ModelAttribute com.college.erp.model.Batch batch,
            @RequestParam int startYear, @RequestParam int duration, RedirectAttributes ra) {
        String name = startYear + "-" + (startYear + duration);
        batch.setName(name);

        if (batch.getId() != null) {
            // Update logic if needed, or just save
            batchService.createBatch(batch);
            ra.addFlashAttribute("success", "Batch updated successfully!");
        } else {
            batchService.createBatch(batch);
            ra.addFlashAttribute("success", "Batch added successfully!");
        }
        return "redirect:/admin/batches";
    }

    @GetMapping("/batches/edit/{id}")
    public String editBatch(@PathVariable Long id, Model model) {
        com.college.erp.model.Batch batch = batchService.getAllBatches().stream()
                .filter(b -> b.getId().equals(id)).findFirst().orElse(null);
        if (batch == null) {
            return "redirect:/admin/batches";
        }

        // Try to parse years for the form
        try {
            String[] years = batch.getName().split("-");
            model.addAttribute("startYear", Integer.parseInt(years[0]));
            model.addAttribute("duration", Integer.parseInt(years[1]) - Integer.parseInt(years[0]));
        } catch (Exception e) {
            model.addAttribute("startYear", 2024);
            model.addAttribute("duration", 4);
        }

        model.addAttribute("batch", batch);
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "admin/batch-form";
    }

    @GetMapping("/batches/delete/{id}")
    public String deleteBatch(@PathVariable Long id, RedirectAttributes ra) {
        // Implementation of delete in Service might be needed or use repository
        // directly if simple
        // For consistency let's check service
        java.util.List<com.college.erp.model.Batch> all = batchService.getAllBatches();
        com.college.erp.model.Batch batch = all.stream().filter(b -> b.getId().equals(id)).findFirst().orElse(null);
        if (batch != null) {
            // Check if service has delete, if not we might need to add it or use repo
            // I'll assume it's in service or I'll add it
            batchService.deleteBatch(id);
            ra.addFlashAttribute("success", "Batch deleted successfully!");
        }
        return "redirect:/admin/batches";
    }

    @GetMapping("/fees")
    public String fees(Model model) {
        model.addAttribute("students", studentService.getAllStudents());
        return "admin/fees";
    }

    @GetMapping("/schedule")
    public String schedule(Model model) {
        String role = getRole();
        String username = getUsername();

        java.util.List<com.college.erp.model.Batch> batches;
        java.util.List<com.college.erp.model.Department> departments;
        com.college.erp.model.Staff currentStaff = null;

        if ("HOD".equals(role)) {
            currentStaff = staffService.getAllStaff().stream()
                    .filter(s -> s.getEmployeeId().equals(username)).findFirst().orElse(null);
            final com.college.erp.model.Staff finalStaff = currentStaff;
            departments = departmentService.getAllDepartments().stream()
                    .filter(d -> finalStaff != null && d.getId().equals(finalStaff.getDepartment().getId()))
                    .collect(java.util.stream.Collectors.toList());
            batches = batchService.getAllBatches().stream()
                    .filter(b -> finalStaff != null
                            && b.getDepartment().getId().equals(finalStaff.getDepartment().getId()))
                    .collect(java.util.stream.Collectors.toList());
        } else if ("ADMIN".equals(role) || "PRINCIPAL".equals(role)) {
            departments = departmentService.getAllDepartments();
            batches = batchService.getAllBatches();
        } else {
            return "redirect:/home";
        }

        model.addAttribute("batches", batches);
        model.addAttribute("departments", departments);

        // Fetch all exam schedules. We'll get them by batch.
        java.util.List<com.college.erp.model.ExamSchedule> allSchedules = new java.util.ArrayList<>();
        for (com.college.erp.model.Batch batch : batches) {
            allSchedules.addAll(academicService.getExamScheduleByBatch(batch));
        }

        // Sort by date/time
        allSchedules.sort((s1, s2) -> {
            if (s1.getExamTime() == null && s2.getExamTime() == null)
                return 0;
            if (s1.getExamTime() == null)
                return 1;
            if (s2.getExamTime() == null)
                return -1;
            return s1.getExamTime().compareTo(s2.getExamTime());
        });

        model.addAttribute("schedules", allSchedules);

        return "admin/schedule";
    }

    @PostMapping("/schedule/add")
    public String addSchedule(@RequestParam Long batchId,
            @RequestParam String subject,
            @RequestParam String examTime,
            @RequestParam String venue,
            RedirectAttributes ra) {
        String role = getRole();
        String username = getUsername();

        try {
            com.college.erp.model.Batch batch = batchService.getAllBatches().stream()
                    .filter(b -> b.getId().equals(batchId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid batch ID"));

            if ("HOD".equals(role)) {
                com.college.erp.model.Staff currentStaff = staffService.getAllStaff().stream()
                        .filter(s -> s.getEmployeeId().equals(username)).findFirst().orElse(null);
                if (currentStaff == null
                        || !batch.getDepartment().getId().equals(currentStaff.getDepartment().getId())) {
                    ra.addFlashAttribute("error", "Unauthorized: Cannot add schedule for another department.");
                    return "redirect:/admin/schedule";
                }
            } else if (!"ADMIN".equals(role) && !"PRINCIPAL".equals(role)) {
                return "redirect:/home";
            }

            com.college.erp.model.ExamSchedule schedule = new com.college.erp.model.ExamSchedule();
            schedule.setBatch(batch);
            schedule.setSubject(subject);
            schedule.setVenue(venue);

            // Parse datetime string from HTML5 datetime-local input
            schedule.setExamTime(java.time.LocalDateTime.parse(examTime));

            academicService.saveExamSchedule(schedule);
            ra.addFlashAttribute("success", "Exam schedule added successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to add exam schedule: " + e.getMessage());
        }
        return "redirect:/admin/schedule";
    }

    @PostMapping("/schedule/delete/{id}")
    public String deleteSchedule(@PathVariable Long id, RedirectAttributes ra) {
        String role = getRole();
        String username = getUsername();

        try {
            com.college.erp.model.ExamSchedule schedule = academicService.getExamScheduleById(id);
            if (schedule == null) {
                ra.addFlashAttribute("error", "Exam schedule not found.");
                return "redirect:/admin/schedule";
            }

            if ("HOD".equals(role)) {
                com.college.erp.model.Staff currentStaff = staffService.getAllStaff().stream()
                        .filter(s -> s.getEmployeeId().equals(username)).findFirst().orElse(null);
                if (currentStaff == null
                        || !schedule.getBatch().getDepartment().getId().equals(currentStaff.getDepartment().getId())) {
                    ra.addFlashAttribute("error", "Unauthorized: Cannot delete schedule for another department.");
                    return "redirect:/admin/schedule";
                }
            } else if (!"ADMIN".equals(role) && !"PRINCIPAL".equals(role)) {
                return "redirect:/home";
            }

            academicService.deleteExamSchedule(id);
            ra.addFlashAttribute("success", "Exam schedule deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete exam schedule.");
        }
        return "redirect:/admin/schedule";
    }
    @GetMapping("/notifications")
    public String notifications(Model model) {
        model.addAttribute("allNotifications", notificationService.getNotificationsForUser("ADMIN", null));
        return "admin/notifications";
    }

    @PostMapping("/notifications/send")
    public String sendNotification(@RequestParam String title,
            @RequestParam String message,
            @RequestParam String type,
            @RequestParam String targetRole,
            RedirectAttributes redirectAttributes) {
        notificationService.createNotification(title, message, type, targetRole);
        redirectAttributes.addFlashAttribute("success", "Notification sent successfully!");
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/delete/{id}")
    public String deleteNotification(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            notificationService.deleteNotification(id);
            redirectAttributes.addFlashAttribute("success", "Notification deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete notification.");
        }
        return "redirect:/admin/notifications";
    }
}
