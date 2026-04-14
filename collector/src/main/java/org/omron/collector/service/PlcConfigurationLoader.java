package org.omron.collector.service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ctrl.extras.Tag;
import org.omron.collector.util.PlcNodeMonitorPanel;

/**
 * Carrega e gerencia configurações de PLC.
 * Responsabilidades:
 * - Carregar configurações do banco (devices, tags)
 * - Construir catálogo de tags conhecidas
 * - Converter dados DB em estruturas de monitoramento
 */
public class PlcConfigurationLoader {

    private final DatabaseManager dbManager;

    public PlcConfigurationLoader(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Carrega todas as configurações de PLC
     */
    public List<PlcConfiguration> loadAllConfigurations() {
        Map<String, DatabaseManager.DeviceConfigData> deviceConfigs = dbManager.loadAllDevicesWithTags();
        List<PlcConfiguration> configs = new ArrayList<>();

        for (DatabaseManager.DeviceConfigData deviceConfig : deviceConfigs.values()) {
            List<PlcNodeMonitorPanel.MonitoredTag> monitoredTags = loadTagsForDevice(deviceConfig.mnemonic);

            PlcConfiguration cfg = new PlcConfiguration(
                    deviceConfig.title,
                    deviceConfig.mnemonic,
                    deviceConfig.description,
                    deviceConfig.nodeId,
                    monitoredTags);

            configs.add(cfg);
        }

        return configs;
    }

    /**
     * Carrega tags para um device específico
     */
    public List<PlcNodeMonitorPanel.MonitoredTag> loadTagsForDevice(String deviceMnemonic) {
        Map<String, DatabaseManager.TagData> tagDataMap = dbManager.loadTagsForDevice(deviceMnemonic);
        Map<String, Tag> catalog = buildTagCatalog();

        List<PlcNodeMonitorPanel.MonitoredTag> tags = new ArrayList<>();

        for (DatabaseManager.TagData tagData : tagDataMap.values()) {
            Tag known = catalog.get(tagData.name);
            int lengthWords = known == null || known.isBit() ? 1 : known.getLengthWords();

            PlcNodeMonitorPanel.MonitoredTag monitored = new PlcNodeMonitorPanel.MonitoredTag(
                    tagData.name,
                    tagData.memoryArea,
                    tagData.address,
                    tagData.bit,
                    lengthWords,
                    tagData.persistHistory);

            tags.add(monitored);
        }

        return tags;
    }

    /**
     * Constrói catálogo de todas as tags conhecidas via reflection
     */
    private Map<String, Tag> buildTagCatalog() {
        Map<String, Tag> out = new HashMap<>();

        Field[] fields = Tag.class.getDeclaredFields();
        for (Field field : fields) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (field.getType() != Tag.class) {
                continue;
            }

            try {
                Object raw = field.get(null);
                if (raw instanceof Tag) {
                    Tag tag = (Tag) raw;
                    out.put(tag.getName(), tag);
                }
            } catch (IllegalAccessException ignored) {
                // ignore and continue
            }
        }

        return out;
    }

    /**
     * Encontra configuração de PLC por ID de node
     */
    public PlcConfiguration findConfigurationByNodeId(List<PlcConfiguration> configs, int nodeId) {
        for (PlcConfiguration cfg : configs) {
            if (cfg.nodeId == nodeId) {
                return cfg;
            }
        }
        return null;
    }

    /**
     * Encontra configuração de PLC por mnemônico
     */
    public PlcConfiguration findConfigurationByMnemonic(List<PlcConfiguration> configs, String mnemonic) {
        for (PlcConfiguration cfg : configs) {
            if (cfg.mnemonic.equalsIgnoreCase(mnemonic)) {
                return cfg;
            }
        }
        return null;
    }

    /**
     * Classe de dados para configuração de PLC
     */
    public static class PlcConfiguration {
        public final String title;
        public final String mnemonic;
        public final String description;
        public final int nodeId;
        public final List<PlcNodeMonitorPanel.MonitoredTag> tags;

        public PlcConfiguration(String title, String mnemonic, String description,
                int nodeId, List<PlcNodeMonitorPanel.MonitoredTag> tags) {
            this.title = title;
            this.mnemonic = mnemonic;
            this.description = description;
            this.nodeId = nodeId;
            this.tags = tags;
        }
    }
}
