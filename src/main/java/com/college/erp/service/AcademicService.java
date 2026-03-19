package com.college.erp.service;

import com.college.erp.model.*;
import com.college.erp.repository.AttendanceRepository;
import com.college.erp.repository.MarksRepository;
import com.college.erp.repository.ExamScheduleRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class AcademicService {

    private final AttendanceRepository attendanceRepository;
    private final MarksRepository marksRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final com.college.erp.repository.TimetableEntryRepository timetableEntryRepository;
    private final com.college.erp.repository.PeriodTimingRepository periodTimingRepository;

    public AcademicService(AttendanceRepository attendanceRepository, MarksRepository marksRepository,
            ExamScheduleRepository examScheduleRepository,
            com.college.erp.repository.TimetableEntryRepository timetableEntryRepository,
            com.college.erp.repository.PeriodTimingRepository periodTimingRepository) {
        this.attendanceRepository = attendanceRepository;
        this.marksRepository = marksRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.periodTimingRepository = periodTimingRepository;
    }

    public void markAttendance(Student student, boolean isPresent, Staff markedBy, int periodNumber) {
        // If attendance for this period exists, update it, else create new
        Attendance attendance = attendanceRepository
                .findByStudentAndDateAndPeriodNumber(student, LocalDate.now(), periodNumber)
                .orElse(Attendance.builder()
                        .student(student)
                        .date(LocalDate.now())
                        .periodNumber(periodNumber)
                        .build());

        attendance.setPresent(isPresent);
        attendance.setMarkedBy(markedBy);

        try {
            attendanceRepository.saveAndFlush(attendance);
            System.out.println("DEBUG - ATTENDANCE SAVED FOR STUDENT: " + student.getId() + " PERIOD: " + periodNumber);
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR SAVING ATTENDANCE: " + e.getMessage());
            throw e;
        }
    }

    public List<Attendance> getAttendanceByStudentAndDate(Student student, LocalDate date) {
        return attendanceRepository.findByStudentAndDate(student, date);
    }

    public List<TimetableEntry> getTimetableByBatchYearAndSem(Batch batch, Integer year, Integer semester) {
        return timetableEntryRepository.findByBatchAndYearAndSemesterOrderByDayOfWeekAscPeriodNumberAsc(batch, year,
                semester);
    }

    public boolean hasAttendancePermission(Staff staff, Batch batch, int periodNumber) {
        if ("HOD".equalsIgnoreCase(staff.getDesignation())) {
            return true;
        }

        String currentDay = LocalDate.now().getDayOfWeek().name();
        List<TimetableEntry> todaysTimetable = timetableEntryRepository
                .findByBatchAndDayOfWeekOrderByPeriodNumberAsc(batch, currentDay);

        for (TimetableEntry entry : todaysTimetable) {
            if (entry.getPeriodNumber() == periodNumber && entry.getStaff() != null && entry.getStaff().getId().equals(staff.getId())) {
                return true;
            }
        }
        return false;
    }

    public void saveTimetableEntry(TimetableEntry entry) {
        String canonicalDay = entry.getDayOfWeek().toUpperCase();
        TimetableEntry existing = timetableEntryRepository
                .findByBatchAndYearAndSemesterAndDayOfWeekAndPeriodNumber(entry.getBatch(), entry.getYear(),
                        entry.getSemester(), canonicalDay, entry.getPeriodNumber())
                .orElse(null);

        if (existing != null) {
            existing.setSubjectName(entry.getSubjectName());
            existing.setStaff(entry.getStaff());
            timetableEntryRepository.save(existing);
        } else {
            entry.setDayOfWeek(canonicalDay);
            timetableEntryRepository.save(entry);
        }
    }

    public void saveTimetableEntries(List<TimetableEntry> entries) {
        for (TimetableEntry entry : entries) {
            saveTimetableEntry(entry);
        }
    }

    public List<PeriodTiming> getPeriodTimings(Department department) {
        return periodTimingRepository.findByDepartmentOrderByPeriodNumberAsc(department);
    }

    public void savePeriodTiming(PeriodTiming timing) {
        java.util.List<PeriodTiming> existingList = periodTimingRepository
                .findByDepartmentAndPeriodNumber(timing.getDepartment(), timing.getPeriodNumber());

        if (!existingList.isEmpty()) {
            // Update the first one
            PeriodTiming existing = existingList.get(0);
            existing.setStartTime(timing.getStartTime());
            existing.setEndTime(timing.getEndTime());
            existing.setBreak(timing.isBreak());
            existing.setBreakName(timing.getBreakName());
            periodTimingRepository.save(existing);

            // Clean up others if any (to prevent future issues)
            if (existingList.size() > 1) {
                for (int i = 1; i < existingList.size(); i++) {
                    periodTimingRepository.delete(existingList.get(i));
                }
            }
        } else {
            periodTimingRepository.save(timing);
        }
    }

    public void savePeriodTimings(java.util.List<PeriodTiming> timings) {
        for (PeriodTiming timing : timings) {
            savePeriodTiming(timing);
        }
    }

    public List<Attendance> getAttendanceByStudent(Student student) {
        return attendanceRepository.findByStudent(student);
    }

    public void enterMarks(Marks marks) {
        marksRepository.save(marks);
    }

    public List<Marks> getMarksByStudent(Student student) {
        return marksRepository.findByStudent(student);
    }

    public List<ExamSchedule> getExamScheduleByBatch(Batch batch) {
        return examScheduleRepository.findByBatch(batch);
    }

    public com.college.erp.model.ExamSchedule getExamScheduleById(Long id) {
        return examScheduleRepository.findById(id).orElse(null);
    }

    public void saveExamSchedule(ExamSchedule examSchedule) {
        examScheduleRepository.save(examSchedule);
    }

    public void deleteExamSchedule(Long id) {
        examScheduleRepository.deleteById(id);
    }

    public Integer getCurrentPeriodNumber(Department department) {
        List<PeriodTiming> timings = getPeriodTimings(department);
        // Force IST timezone for matching with local college timings
        java.time.ZonedDateTime nowIST = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        LocalTime now = nowIST.toLocalTime();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

        for (PeriodTiming timing : timings) {
            try {
                // Remove potential extra spaces and fix "09:00" to "9:00" if needed for 'h:mm'
                String startTimeStr = timing.getStartTime().trim().toUpperCase().replaceFirst("^0", "");
                String endTimeStr = timing.getEndTime().trim().toUpperCase().replaceFirst("^0", "");
                
                LocalTime start = LocalTime.parse(startTimeStr, formatter);
                LocalTime end = LocalTime.parse(endTimeStr, formatter);

                if (!now.isBefore(start) && !now.isAfter(end)) {
                    return timing.getPeriodNumber();
                }
            } catch (Exception e) {
                System.err.println("Error parsing time for period " + timing.getPeriodNumber() + ": " + e.getMessage());
            }
        }
        return null;
    }
}
