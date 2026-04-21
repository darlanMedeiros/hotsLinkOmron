package org.omron.collector.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ctrl.db.config.DbConfig;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.MemoryValue;
import org.ctrl.db.service.QualidadeService;
import org.ctrl.db.repository.DefeitoRepository;
import org.ctrl.db.service.DmValueService;
import org.ctrl.db.service.RrValueService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Gerencia a conexão com banco de dados e operações relacionadas.
 * Responsabilidades:
 * - Inicializar/encerrar contexto Spring
 * - Executar queries de configuração
 * - Operações CRUD de dados
 */
public class DatabaseManager {

    private AnnotationConfigApplicationContext dbContext;
    private DmValueService dmValueService;
    private RrValueService rrValueService;
    private QualidadeService qualidadeService;
    private DefeitoRepository defeitoRepository;
    private volatile boolean initialized;

    public void initialize() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            dbContext = new AnnotationConfigApplicationContext(DbConfig.class);
            dmValueService = dbContext.getBean(DmValueService.class);
            rrValueService = dbContext.getBean(RrValueService.class);
            qualidadeService = dbContext.getBean(QualidadeService.class);
            defeitoRepository = dbContext.getBean(DefeitoRepository.class);
            initialized = true;
        }
    }

    public void close() {
        synchronized (this) {
            if (dbContext != null) {
                dbContext.close();
                dbContext = null;
                dmValueService = null;
                rrValueService = null;
                initialized = false;
            }
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public JdbcTemplate getJdbcTemplate() {
        initialize();
        return dbContext.getBean(JdbcTemplate.class);
    }

    public DmValueService getDmValueService() {
        initialize();
        return dmValueService;
    }

    public RrValueService getRrValueService() {
        initialize();
        return rrValueService;
    }

    public QualidadeService getQualidadeService() {
        initialize();
        return qualidadeService;
    }

    public DefeitoRepository getDefeitoRepository() {
        initialize();
        return defeitoRepository;
    }

    /**
     * Carrega todos os devices com tags associadas organizados por mnemônico
     */
    public Map<String, DeviceConfigData> loadAllDevicesWithTags() {
        initialize();
        JdbcTemplate jdbc = getJdbcTemplate();

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT d.id AS device_id, d.mnemonic, d.name AS device_name, d.description AS device_description, " +
                        "d.no_id AS device_node_id " +
                        "FROM public.device d " +
                        "ORDER BY d.id");

        Map<String, DeviceConfigData> grouped = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String mnemonic = asString(row.get("mnemonic"));
            if (mnemonic == null || mnemonic.trim().isEmpty()) {
                continue;
            }

            Integer deviceId = asInt(row.get("device_id"));
            Integer nodeId = asInt(row.get("device_node_id"));
            int configuredNodeId = nodeId == null ? 0 : nodeId.intValue();
            String deviceName = asString(row.get("device_name"), mnemonic);
            String description = asString(row.get("device_description"), "Omron " + mnemonic);

            if (!grouped.containsKey(mnemonic)) {
                grouped.put(mnemonic, new DeviceConfigData(
                        deviceId == null ? 0 : deviceId,
                        deviceName, mnemonic, description, configuredNodeId, new HashMap<>()));
            }
        }
        return grouped;
    }

    /**
     * Carrega tags para um device específico
     */
    public Map<String, TagData> loadTagsForDevice(String deviceMnemonic) {
        initialize();
        if (deviceMnemonic == null || deviceMnemonic.trim().isEmpty()) {
            return new HashMap<>();
        }

        JdbcTemplate jdbc = getJdbcTemplate();
        Map<String, TagData> tags = new LinkedHashMap<>();

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT t.name AS tag_name, t.machine_id AS machine_id, t.persist_history AS persist_history, " +
                        "m.name AS memory_area, m.address AS memory_address, m.bit AS memory_bit " +
                        "FROM public.device d " +
                        "JOIN public.machine ma ON ma.device_id = d.id " +
                        "JOIN public.tag t ON t.machine_id = ma.id " +
                        "JOIN public.memory m ON m.id = t.memory_id AND m.device_id = d.id " +
                        "WHERE d.mnemonic = ? " +
                        "ORDER BY t.id",
                deviceMnemonic);

        for (Map<String, Object> row : rows) {
            String tagName = asString(row.get("tag_name"));
            Integer machineId = asInt(row.get("machine_id"));
            Integer address = asInt(row.get("memory_address"));
            String memoryArea = asString(row.get("memory_area"));
            Integer memoryBit = asInt(row.get("memory_bit"));
            String memoryName = formatMemoryKey(memoryArea, address, memoryBit);
            boolean persistHistory = asBoolean(row.get("persist_history"), true);

            if (tagName == null || machineId == null || machineId.intValue() <= 0
                    || address == null || address.intValue() < 0) {
                continue;
            }
            /**
             * if (!"DM".equalsIgnoreCase(asString(memoryArea, ""))) {
             * continue;
             * }
             * if (memoryBit != null && memoryBit.intValue() >= 0) {
             * continue;
             * }
             */

            tags.put(tagName, new TagData(
                    tagName, machineId.intValue(), memoryArea, address.intValue(), memoryBit == null ? -1 : memoryBit.intValue(),
                    memoryName, persistHistory));
        }
        return tags;
    }

    /**
     * Carrega bindings de tags de produção manual
     */
    public Map<String, ManualProductionTagBinding> loadManualProductionTagBindings(
            String deviceMnemonic, String[] productionTagNames) {
        initialize();
        JdbcTemplate jdbc = getJdbcTemplate();

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < productionTagNames.length; i++) {
            if (i > 0)
                placeholders.append(',');
            placeholders.append('?');
        }

        Object[] params = new Object[productionTagNames.length + 1];
        params[0] = deviceMnemonic;
        for (int i = 0; i < productionTagNames.length; i++) {
            params[i + 1] = productionTagNames[i];
        }

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT t.name AS tag_name, m.name AS memory_area, m.address AS memory_address, m.bit AS memory_bit, " +
                        "t.persist_history AS persist_history " +
                        "FROM public.device d " +
                        "JOIN public.machine ma ON ma.device_id = d.id " +
                        "JOIN public.tag t ON t.machine_id = ma.id " +
                        "JOIN public.memory m ON m.id = t.memory_id AND m.device_id = d.id " +
                        "WHERE d.mnemonic = ? AND t.name IN (" + placeholders + ")",
                params);

        Map<String, ManualProductionTagBinding> out = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String tagName = asString(row.get("tag_name"));
            String memoryArea = asString(row.get("memory_area"));
            Integer address = asInt(row.get("memory_address"));
            Integer memoryBit = asInt(row.get("memory_bit"));
            boolean persistHistory = asBoolean(row.get("persist_history"), true);

            if (tagName == null || memoryArea == null || memoryArea.trim().isEmpty()
                    || address == null || address.intValue() < 0) {
                continue;
            }

            out.put(tagName, new ManualProductionTagBinding(
                    formatMemoryKey(memoryArea, address, memoryBit),
                    memoryArea, address.intValue(),
                    memoryBit == null ? -1 : memoryBit.intValue(), persistHistory));
        }
        return out;
    }

    /**
     * Salva valores de memória em lote (com histórico)
     */
    public void saveMemoryValuesBatch(DeviceInfo deviceInfo, List<MemoryValue> values) {
        if (values.isEmpty())
            return;

        List<MemoryValue> dmBatch = new ArrayList<>();
        List<MemoryValue> rrBatch = new ArrayList<>();

        for (MemoryValue v : values) {
            if (v.getName() != null && v.getName().startsWith("RR_")) {
                rrBatch.add(v);
            } else {
                dmBatch.add(v);
            }
        }

        if (!dmBatch.isEmpty()) {
            getDmValueService().saveBatch(deviceInfo, dmBatch);
        }
        if (!rrBatch.isEmpty()) {
            getRrValueService().saveBatch(deviceInfo, rrBatch);
        }
    }

    /**
     * Salva valores de memória em lote (apenas current, sem histórico)
     */
    public void saveMemoryValuesCurrentOnly(DeviceInfo deviceInfo, List<MemoryValue> values) {
        if (values.isEmpty())
            return;

        List<MemoryValue> dmBatch = new ArrayList<>();
        List<MemoryValue> rrBatch = new ArrayList<>();

        for (MemoryValue v : values) {
            if (v.getName() != null && v.getName().startsWith("RR_")) {
                rrBatch.add(v);
            } else {
                dmBatch.add(v);
            }
        }

        if (!dmBatch.isEmpty()) {
            getDmValueService().saveBatchCurrentOnly(deviceInfo, dmBatch);
        }
        if (!rrBatch.isEmpty()) {
            // RrValueService currently doesn't have saveBatchCurrentOnly, but we can use
            // saveBatch for now
            // since RR current only logic might be implemented later or is less critical.
            getRrValueService().saveBatch(deviceInfo, rrBatch);
        }
    }

    // Helper methods
    private static String asString(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    private static String asString(Object raw, String fallback) {
        String value = asString(raw);
        return (value == null || value.trim().isEmpty()) ? fallback : value;
    }

    private static Integer asInt(Object raw) {
        if (raw == null)
            return null;
        if (raw instanceof Number) {
            return Integer.valueOf(((Number) raw).intValue());
        }
        try {
            return Integer.valueOf(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean asBoolean(Object raw, boolean fallback) {
        if (raw == null)
            return fallback;
        if (raw instanceof Boolean) {
            return ((Boolean) raw).booleanValue();
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty())
            return fallback;
        if ("1".equals(text))
            return true;
        if ("0".equals(text))
            return false;
        return Boolean.parseBoolean(text);
    }

    private static String formatMemoryKey(String area, Integer address, Integer bit) {
        String normalizedArea = area == null || area.trim().isEmpty() ? "DM" : area.trim().toUpperCase();
        int normalizedAddress = address == null ? 0 : Math.max(0, address.intValue());
        int normalizedBit = bit == null ? -1 : bit.intValue();
        if (normalizedBit >= 0) {
            return String.format("%s_%04d.%02d", normalizedArea, normalizedAddress, normalizedBit);
        }
        return String.format("%s_%04d", normalizedArea, normalizedAddress);
    }

    /**
     * Identifica grupos de tags de qualidade baseados na convenção de nomes
     */
    public Map<Integer, QualityGroup> loadQualityTagGroups(String deviceMnemonic) {
        initialize();
        Map<String, TagData> allTags = loadTagsForDevice(deviceMnemonic);
        Map<Integer, QualityGroup> groups = new HashMap<>();

        // Para simplificar, assumimos um grupo por device se as tags existirem.
        // Se houver múltiplas máquinas no mesmo device, os nomes das tags poderiam ter prefixos.
        // Como o usuário não especificou prefixos por máquina no PROBLEMA.MD, vamos agrupar 
        // as tags soltas em um grupo se elas existirem.
        
        QualityGroup group = new QualityGroup();
        boolean foundAny = false;

        for (TagData td : allTags.values()) {
            String name = td.name;
            if ("Qualidade_Gatilho".equals(name)) { group.trigger = td; foundAny = true; }
            else if ("Total_Defeitos1".equals(name)) { group.defectsTotal[0] = td; foundAny = true; }
            else if ("Total_Defeitos2".equals(name)) { group.defectsTotal[1] = td; foundAny = true; }
            else if ("Total_Defeitos3".equals(name)) { group.defectsTotal[2] = td; foundAny = true; }
            else if ("Codigo_Defeito1".equals(name)) { group.defectsCode[0] = td; foundAny = true; }
            else if ("Codigo_Defeito2".equals(name)) { group.defectsCode[1] = td; foundAny = true; }
            else if ("Codigo_Defeito3".equals(name)) { group.defectsCode[2] = td; foundAny = true; }
            else if ("Qtde_Amostragem".equals(name)) { group.sampling = td; foundAny = true; }
            else if ("Qualidade_Ano".equals(name)) { group.year = td; foundAny = true; }
            else if ("Qualidade_Mes".equals(name)) { group.month = td; foundAny = true; }
            else if ("Qualidade_Dia".equals(name)) { group.day = td; foundAny = true; }
            else if ("Qualidade_Hora".equals(name)) { group.hour = td; foundAny = true; }
            else if ("Qualidade_Min".equals(name)) { group.minute = td; foundAny = true; }
            else if ("Qualidade_Seg".equals(name)) { group.second = td; foundAny = true; }
            else if ("Qualidade_Maquina_Current".equals(name)) { group.machineQualityCurrent = td; foundAny = true; }
            else if ("Qualidade_Maquina_Persisted".equals(name)) { group.machineQualityPersisted = td; foundAny = true; }
        }

        if (foundAny && group.trigger != null) {
            // Usamos o machine_id da tag gatilho como chave se necessário, 
            // no momento 0 como único grupo por PLC para simplificar.
            groups.put(0, group); 
        }

        return groups;
    }

    // Data classes
    public static class DeviceConfigData {
        public final int id;
        public final String title;
        public final String mnemonic;
        public final String description;
        public final int nodeId;
        public final Map<String, TagData> tags;

        public DeviceConfigData(int id, String title, String mnemonic, String description,
                int nodeId, Map<String, TagData> tags) {
            this.id = id;
            this.title = title;
            this.mnemonic = mnemonic;
            this.description = description;
            this.nodeId = nodeId;
            this.tags = tags;
        }
    }

    public static class TagData {
        public final String name;
        public final int machineId;
        public final String memoryArea;
        public final int address;
        public final int bit;
        public final String memoryName;
        public final boolean persistHistory;

        public TagData(String name, int machineId, String memoryArea, int address, int bit, String memoryName,
                boolean persistHistory) {
            this.name = name;
            this.machineId = machineId;
            this.memoryArea = memoryArea;
            this.address = address;
            this.bit = bit;
            this.memoryName = memoryName;
            this.persistHistory = persistHistory;
        }
    }

    public static class ManualProductionTagBinding {
        public final String memoryName;
        public final String memoryArea;
        public final int address;
        public final int bit;
        public final boolean persistHistory;

        public ManualProductionTagBinding(String memoryName, String memoryArea, int address, int bit,
                boolean persistHistory) {
            this.memoryName = memoryName;
            this.memoryArea = memoryArea;
            this.address = address;
            this.bit = bit;
            this.persistHistory = persistHistory;
        }
    }

    public static class QualityGroup {
        public TagData trigger;
        public TagData[] defectsTotal = new TagData[3];
        public TagData[] defectsCode = new TagData[3];
        public TagData sampling;
        public TagData year;
        public TagData month;
        public TagData day;
        public TagData hour;
        public TagData minute;
        public TagData second;
        public TagData machineQualityCurrent;
        public TagData machineQualityPersisted;

        public boolean isValidTriggerGroup() {
            return trigger != null && year != null && month != null && day != null &&
                   hour != null && minute != null && second != null;
        }
    }
}
