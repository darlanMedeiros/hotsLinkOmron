package org.omron.collector.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

/**
 * Centraliza todas as operações de logging (UI e persistência em arquivo).
 * Responsabilidades:
 * - Logging em área de texto Swing
 * - Persistência em arquivo
 * - Trimming de linhas quando limite é atingido
 */
public class LoggingService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_LOG_LINES = 4000;
    private static final Path LOG_FILE = Path.of("collector-mult-plc.log");

    private JTextArea logArea;
    private Consumer<String> externalCallback;

    public LoggingService(JTextArea logArea) {
        this.logArea = logArea;
    }

    public LoggingService(JTextArea logArea, Consumer<String> externalCallback) {
        this.logArea = logArea;
        this.externalCallback = externalCallback;
    }

    public void log(String message) {
        appendPersistentLog(message, null);
        logToUi(message);
        if (externalCallback != null) {
            externalCallback.accept("LOG: " + message);
        }
    }

    public void logError(String context, Throwable error) {
        String message = context + ": " + describeError(error);
        appendPersistentLog(message, error);
        logToUi(message);
        if (externalCallback != null) {
            externalCallback.accept("ERROR: " + message);
        }
    }

    public void logInfo(String message) {
        log(message);
    }

    public void logWarning(String message) {
        String msg = "[AVISO] " + message;
        appendPersistentLog(msg, null);
        logToUi(msg);
        if (externalCallback != null) {
            externalCallback.accept("WARN: " + message);
        }
    }

    private void logToUi(String message) {
        runOnEdt(() -> {
            if (logArea == null) {
                return;
            }
            String time = LocalDateTime.now().format(TIME_FMT);
            logArea.append("[" + time + "] " + message + "\n");
            trimLogIfNeeded();
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void trimLogIfNeeded() {
        if (logArea == null) {
            return;
        }
        int lineCount = logArea.getLineCount();
        if (lineCount <= MAX_LOG_LINES) {
            return;
        }
        int linesToRemove = lineCount - MAX_LOG_LINES;
        try {
            int endOffset = logArea.getLineEndOffset(linesToRemove - 1);
            logArea.replaceRange("", 0, endOffset);
        } catch (BadLocationException ignored) {
            // Keep application running even if line offsets mismatch momentarily.
        }
    }

    private static synchronized void appendPersistentLog(String message, Throwable error) {
        try {
            StringBuilder line = new StringBuilder();
            line.append("[")
                    .append(LocalDateTime.now().format(FILE_TIME_FMT))
                    .append("] ")
                    .append(message)
                    .append(System.lineSeparator());

            if (error != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                error.printStackTrace(pw);
                pw.flush();
                line.append(sw).append(System.lineSeparator());
            }

            Files.writeString(
                    LOG_FILE,
                    line.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // Logging should not break the application flow.
        }
    }

    private static String describeError(Throwable error) {
        if (error == null) {
            return "erro desconhecido";
        }
        String type = error.getClass().getSimpleName();
        String text = error.getMessage();
        if (text == null || text.trim().isEmpty()) {
            return type;
        }
        return type + " - " + text;
    }

    private static void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
            return;
        }
        SwingUtilities.invokeLater(task);
    }

    public void setLogArea(JTextArea area) {
        this.logArea = area;
    }

    public void setExternalCallback(Consumer<String> callback) {
        this.externalCallback = callback;
    }

    public static void installGlobalHandler(Consumer<String> logCallback) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            String message = "Uncaught exception thread=" + thread.getName() + ": " + describeError(error);
            appendPersistentLog(message, error);
            if (logCallback != null) {
                logCallback.accept("GLOBAL_ERROR: " + message);
            }
        });
    }
}
