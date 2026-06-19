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
        private Boolean ok;
        private String serverId;
    }
}
