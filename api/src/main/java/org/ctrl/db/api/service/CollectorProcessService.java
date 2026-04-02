package org.ctrl.db.api.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.ctrl.db.api.dto.CollectorStatusResponse;
import org.springframework.stereotype.Service;

@Service
public class CollectorProcessService {

    private static final int MAX_LOG_LINES = 200;
    private static final int STOP_TIMEOUT_SECONDS = 5;

    private final Object lock = new Object();
    private final Deque<String> recentLogs = new ArrayDeque<>();

    private Process process;
    private Instant startedAt;
    private Instant stoppedAt;
    private Integer exitCode;
    private String lastError;
    private String command;
    private String workingDirectory;

    public CollectorStatusResponse status() {
        synchronized (lock) {
            refreshProcessStateLocked();
            return snapshotLocked();
        }
    }

    public CollectorStatusResponse start() {
        synchronized (lock) {
            refreshProcessStateLocked();
            if (isRunningLocked()) {
                return snapshotLocked();
            }

            Path scriptPath = resolveScriptPath();
            List<String> commandParts = List.of(
                    "powershell",
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    scriptPath.toAbsolutePath().toString());

            try {
                ProcessBuilder builder = new ProcessBuilder(commandParts);
                builder.directory(scriptPath.getParent().toFile());
                builder.redirectErrorStream(true);

                process = builder.start();
                startedAt = Instant.now();
                stoppedAt = null;
                exitCode = null;
                lastError = null;
                command = String.join(" ", commandParts);
                workingDirectory = scriptPath.getParent().toAbsolutePath().toString();
                recentLogs.clear();

                startLogPump(process);
                startExitWatcher(process);
            } catch (IOException ex) {
                lastError = "Falha ao iniciar collector: " + ex.getMessage();
                throw new IllegalArgumentException(lastError, ex);
            }

            return snapshotLocked();
        }
    }

    public CollectorStatusResponse stop() {
        Process current;
        synchronized (lock) {
            refreshProcessStateLocked();
            if (!isRunningLocked()) {
                return snapshotLocked();
            }
            current = process;
            appendLogLocked("Solicitacao de parada recebida.");
        }

        if (current != null) {
            current.destroy();
            try {
                if (!current.waitFor(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    current.destroyForcibly();
                    current.waitFor(STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        synchronized (lock) {
            refreshProcessStateLocked();
            return snapshotLocked();
        }
    }

    private Path resolveScriptPath() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                cwd.resolve("run-collector.ps1"),
                cwd.resolve("..").resolve("run-collector.ps1"));

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.normalize();
            }
        }

        throw new IllegalArgumentException(
                "Arquivo run-collector.ps1 nao encontrado. Verifique se a API esta sendo executada na raiz do projeto.");
    }

    private void startLogPump(Process target) {
        Thread logThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(target.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (lock) {
                        appendLogLocked(line);
                    }
                }
            } catch (IOException ex) {
                synchronized (lock) {
                    appendLogLocked("Falha ao ler logs do collector: " + ex.getMessage());
                }
            }
        }, "collector-process-log");
        logThread.setDaemon(true);
        logThread.start();
    }

    private void startExitWatcher(Process target) {
        Thread watcher = new Thread(() -> {
            try {
                int code = target.waitFor();
                synchronized (lock) {
                    if (process == target) {
                        exitCode = code;
                        stoppedAt = Instant.now();
                        process = null;
                        appendLogLocked("Collector finalizado com exit code " + code + ".");
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }, "collector-process-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    private void refreshProcessStateLocked() {
        if (process != null && !process.isAlive()) {
            exitCode = process.exitValue();
            stoppedAt = Instant.now();
            process = null;
        }
    }

    private boolean isRunningLocked() {
        return process != null && process.isAlive();
    }

    private CollectorStatusResponse snapshotLocked() {
        boolean running = isRunningLocked();
        Long pid = running && process != null ? process.pid() : null;

        return new CollectorStatusResponse(
                running,
                pid,
                exitCode,
                startedAt,
                stoppedAt,
                command,
                workingDirectory,
                lastError,
                new ArrayList<>(recentLogs));
    }

    private void appendLogLocked(String line) {
        if (line == null) {
            return;
        }
        recentLogs.addLast(line);
        while (recentLogs.size() > MAX_LOG_LINES) {
            recentLogs.removeFirst();
        }
    }
}
