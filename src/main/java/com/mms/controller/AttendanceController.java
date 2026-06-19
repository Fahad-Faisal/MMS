package com.mms.controller;

import com.mms.dto.request.AttendanceBatchRequest;
import com.mms.entity.Attendance;
import com.mms.enums.AttendanceSession;
import com.mms.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;

    @GetMapping
    public ResponseEntity<List<Attendance>> getAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AttendanceSession session,
            @RequestParam(required = false) String className) {
        return ResponseEntity.ok(attendanceService.getAttendance(date, session, className));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Integer>> saveAttendanceBatch(
            @Valid @RequestBody AttendanceBatchRequest request) {
        Map<String, Integer> result = attendanceService.saveBatch(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncAttendance(@RequestBody Map<String, Object> request) {
        // Handle offline sync
        return ResponseEntity.ok(attendanceService.syncAttendance(request));
    }
}
