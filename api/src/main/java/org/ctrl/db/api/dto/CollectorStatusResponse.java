package org.ctrl.db.api.dto;

import java.time.Instant;
import java.util.List;

public record CollectorStatusResponse(
        boolean running,
        Long pid,
        Integer exitCode,
        Instant startedAt,
        Instant stoppedAt,
        String command,
        String workingDirectory,
        String lastError,
        List<String> recentLogs) {
}
