// src/main/java/com/mms/dto/response/AttendanceSyncResponse.java
package com.mms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AttendanceSyncResponse {
    private List<SyncResult> results;

    @Data
    @Builder
    public static class SyncResult {
        private String clientId;
        private Boolean success;
        private String serverId;
        private String message;
    }
}