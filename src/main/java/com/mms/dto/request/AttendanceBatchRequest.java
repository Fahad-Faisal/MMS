// src/main/java/com/mms/dto/request/AttendanceBatchRequest.java
package com.mms.dto.request;

import com.mms.enums.AttendanceSession;
import com.mms.enums.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AttendanceBatchRequest {
    @NotNull
    private LocalDate date;

    @NotNull
    private AttendanceSession session;

    private String className;

    private String classroomId;  // NEW

    @NotEmpty
    private List<AttendanceRecord> records;

    @Data
    public static class AttendanceRecord {
        @NotBlank
        private String studentId;

        @NotNull
        private AttendanceStatus status;

        private String note;
    }
}