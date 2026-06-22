// src/main/java/com/mms/dto/request/AttendanceSyncRequest.java
package com.mms.dto.request;

import com.mms.enums.AttendanceSession;
import com.mms.enums.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AttendanceSyncRequest {
    @NotEmpty(message = "At least one batch is required")
    @Valid
    private List<SyncBatch> batches;

    @Data
    public static class SyncBatch {
        @NotNull
        private LocalDate date;

        @NotNull
        private AttendanceSession session;

        private String className;

        private String classroomId;

        @NotEmpty
        @Valid
        private List<SyncRecord> records;

        private LocalDateTime queuedAt;

        @Data
        public static class SyncRecord {
            private String clientId;  // Client-generated UUID for deduplication

            @NotNull
            private String studentId;

            @NotNull
            private AttendanceStatus status;

            private String note;

            private LocalDateTime recordedAt;
        }
    }
}