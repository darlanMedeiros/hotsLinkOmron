package org.omron.collector.service;

import java.util.function.Consumer;
import javax.swing.JComboBox;
import org.ctrl.comm.serial.SerialPortAbstract;
import org.omron.collector.util.SharedSerial;

/**
 * Gerencia a comunicação serial compartilhada entre múltiplos nodes PLC.
 * Responsabilidades:
 * - Conectar/desconectar porta serial
 * - Gerenciar estado da conexão
 * - Validar configurações de porta
 */
public class SerialCommunicationManager {

    private final SharedSerial sharedSerial = new SharedSerial();
    private volatile boolean isConnected;
    private volatile boolean forcedDisconnectInProgress;
    private Consumer<String> statusCallback;
    private Consumer<String> logCallback;

    public SerialCommunicationManager(Consumer<String> statusCallback, Consumer<String> logCallback) {
        this.statusCallback = statusCallback;
        this.logCallback = logCallback;
    }

    public SharedSerial getSharedSerial() {
        return sharedSerial;
    }

    public boolean isConnected() {
        return isConnected && sharedSerial.isConnected();
    }

    public void connect(String port, int baud, int dataBits, int stopBits,
            SerialPortAbstract.Parity parity, int timeout,
            boolean rts, boolean dtr) throws Exception {
        if (isConnected()) {
            log("Comunicacao serial compartilhada ja esta ativa.");
            return;
        }

        if (port == null || port.trim().isEmpty()) {
            updateStatus("PORTA INVALIDA");
            log("Selecione uma porta serial valida.");
            throw new IllegalArgumentException("Porta serial invalida");
        }

        try {
            SharedSerial.Config config = new SharedSerial.Config(port, baud, dataBits, stopBits, parity, timeout, rts,
                    dtr);
            sharedSerial.connect(config);
            isConnected = true;
            updateStatus("CONECTADO");
            log("Comunicacao serial compartilhada conectada na porta " + port + ".");
        } catch (Exception ex) {
            isConnected = false;
            handleConnectionError(ex, port);
            throw ex;
        }
    }

    public void disconnect() {
        if (!isConnected()) {
            return;
        }
        safeStop();
        isConnected = false;
        updateStatus("DESCONECTADO");
        log("Comunicacao serial compartilhada encerrada.");
    }

    public void requestDisconnectByUnresponsiveNode(int nodeId) {
        if (!isConnected()) {
            return;
        }
        synchronized (this) {
            if (forcedDisconnectInProgress) {
                return;
            }
            forcedDisconnectInProgress = true;
        }
        Thread disconnectThread = new Thread(() -> {
            try {
                log("[Node " + nodeId
                        + "] Sem resposta prolongada. Desconectando serial compartilhada para liberar a porta.");
                disconnect();
            } finally {
                forcedDisconnectInProgress = false;
            }
        }, "collector-force-shared-disconnect");
        disconnectThread.setDaemon(true);
        disconnectThread.start();
    }

    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    public void setLogCallback(Consumer<String> callback) {
        this.logCallback = callback;
    }

    private void safeStop() {
        if (sharedSerial != null && sharedSerial.isConnected()) {
            try {
                sharedSerial.disconnect();
            } catch (Exception ex) {
                logError("Erro ao desconectar serial", ex);
            }
        }
    }

    private void updateStatus(String status) {
        if (statusCallback != null) {
            statusCallback.accept(status);
        }
    }

    private void log(String message) {
        if (logCallback != null) {
            logCallback.accept(message);
        }
    }

    private void logError(String context, Throwable error) {
        String message = context + ": " + describeError(error);
        if (logCallback != null) {
            logCallback.accept(message);
        }
    }

    private void handleConnectionError(Exception ex, String port) {
        updateStatus("ERRO");
        if (isSerialPortInUse(ex)) {
            updateStatus("PORTA EM USO");
            logError("Porta serial " + port + " em uso por outro processo", ex);
        } else {
            logError("Falha na conexao serial", ex);
        }
        safeStop();
    }

    private static boolean isSerialPortInUse(Throwable error) {
        return SharedSerial.isSerialPortInUse(error);
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

    public static void refreshAvailablePorts(JComboBox<String> portCombo, String preferred, String fallback) {
        try {
            SharedSerial.refreshAvailablePorts(portCombo, preferred, fallback);
        } catch (Exception ex) {
            // On failure do not block UI; ports may not be available.
            // caller should handle the case where port list remains empty.
        }
    }

    public static boolean isPortAvailable(String portName) {
        try {
            return SharedSerial.isPortAvailable(portName);
        } catch (Exception ex) {
            return false;
        }
    }

    public static String getSelectedPortName(JComboBox<String> portCombo) {
        return SharedSerial.getSelectedPortName(portCombo);
    }
}
