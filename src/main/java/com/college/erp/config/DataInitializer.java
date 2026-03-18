package com.college.erp.config;

import com.college.erp.model.Department;
import com.college.erp.model.Batch;
import com.college.erp.service.DepartmentService;
import com.college.erp.service.BatchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private final DepartmentService departmentService;
    private final BatchService batchService;
    private final com.college.erp.service.AcademicService academicService;
    private final com.college.erp.repository.StaffRepository staffRepository;

    public DataInitializer(DepartmentService departmentService, BatchService batchService,
            com.college.erp.service.AcademicService academicService,
            com.college.erp.repository.StaffRepository staffRepository) {
        this.departmentService = departmentService;
        this.batchService = batchService;
        this.academicService = academicService;
        this.staffRepository = staffRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (departmentService.getAllDepartments().isEmpty()) {
            Department cs = Department.builder().code("CS").name("Computer Science").build();
            departmentService.createDepartment(cs);
            departmentService
                    .createDepartment(Department.builder().code("ECE").name("Electronics and Communication").build());
            departmentService
                    .createDepartment(Department.builder().code("ME").name("Mechanical Engineering").build());

            Batch batch = Batch.builder().name("2022-2026").department(cs).build();
            batchService.createBatch(batch);
            batchService.createBatch(Batch.builder().name("2023-2027").department(cs).build());

            // Seed Period Timings (1-11)
            String[] starts = {"09:00 AM", "09:50 AM", "10:40 AM", "11:00 AM", "11:50 AM", "12:40 PM", "01:30 PM", "02:20 PM", "03:10 PM", "04:00 PM", "04:50 PM"};
            String[] ends = {"09:50 AM", "10:40 AM", "11:00 AM", "11:50 AM", "12:40 PM", "01:30 PM", "02:20 PM", "03:10 PM", "04:00 PM", "04:50 PM", "05:40 PM"};
            
            for (int i = 0; i < 11; i++) {
                boolean isBreak = (i == 2 || i == 5 || i == 8);
                String breakName = "";
                if (i == 2 || i == 8) breakName = "Break";
                if (i == 5) breakName = "Lunch";

                academicService.savePeriodTiming(com.college.erp.model.PeriodTiming.builder()
                        .department(cs).periodNumber(i + 1)
                        .startTime(starts[i]).endTime(ends[i])
                        .isBreak(isBreak).breakName(breakName).build());
            }

            // Seed some Timetable Entries if a staff exists
            staffRepository.findAll().stream().findFirst().ifPresent(staff -> {
                academicService.saveTimetableEntry(com.college.erp.model.TimetableEntry.builder()
                        .batch(batch).dayOfWeek("MONDAY").periodNumber(1)
                        .subjectName("Operating Systems").staff(staff).build());
                academicService.saveTimetableEntry(com.college.erp.model.TimetableEntry.builder()
                        .batch(batch).dayOfWeek("TUESDAY").periodNumber(2)
                        .subjectName("Database Systems").staff(staff).build());
            });
        }
    }
}
