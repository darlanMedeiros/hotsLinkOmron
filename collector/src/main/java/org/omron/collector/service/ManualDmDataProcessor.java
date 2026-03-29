package org.omron.collector.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Processa dados de leitura manual de DM.
 * Responsabilidades:
 * - Parsing de respostas de leitura DM
 * - Detecção de timestamps
 * - Validação de marcadores
 * - Conversão BCD
 */
public class ManualDmDataProcessor {

    private static final DateTimeFormatter MANUAL_DM_DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MANUAL_DM_GROUP_SIZE = 13;
    private static final int MANUAL_DM_TAG_VALUES_OFFSET = 8;

    /**
     * Extrai um valor de palavra (16 bits) de uma resposta
     */
    public static int parseSingleWord(Object reply) {
        if (reply == null) {
            throw new IllegalStateException("Resposta vazia na leitura DM manual.");
        }

        // Se replay tem método toHexArray (IData interface)
        int[] dataBuff = null;
        try {
            dataBuff = (int[]) reply.getClass().getMethod("toHexArray").invoke(reply);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao extrair array de resposta", ex);
        }

        if (dataBuff == null || dataBuff.length < 4) {
            throw new IllegalStateException("Resposta invalida na leitura DM manual.");
        }

        StringBuilder value = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            value.append((char) dataBuff[i]);
        }

        String normalized = value.toString().trim();
        if (normalized.isEmpty()) {
            throw new IllegalStateException("Resposta sem conteudo na leitura DM manual.");
        }

        return Integer.parseInt(normalized, 16) & 0xFFFF;
    }

    /**
     * Converte valor inteiro para string hexadecimal de 4 dígitos
     */
    public static String toHexWord(int value) {
        return String.format("%04X", Integer.valueOf(value & 0xFFFF));
    }

    /**
     * Detecta se endereço/valor formam um marcador de data válido
     */
    public static boolean matchesManualDateMarker(int markerAddress, int markerValue) {
        int normalizedValue = markerValue & 0xFFFF;

        // Verifica se valor == endereço
        if (normalizedValue == markerAddress) {
            return true;
        }

        // Verifica se valor (hex) == endereço (decimal convertido para hex)
        String addressDigits = Integer.toString(markerAddress);
        if (addressDigits.length() <= 4) {
            try {
                int hexFromAddressDigits = Integer.parseInt(addressDigits, 16) & 0xFFFF;
                if (normalizedValue == hexFromAddressDigits) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return false;
    }

    /**
     * Detecta se índice/valor formam um marcador relativo válido
     */
    public static boolean matchesManualRelativeMarker(int markerIndex, int markerValue) {
        int normalizedValue = markerValue & 0xFFFF;

        if (normalizedValue == markerIndex) {
            return true;
        }

        String indexDigits = Integer.toString(markerIndex);
        if (indexDigits.length() <= 4) {
            try {
                int hexFromIndexDigits = Integer.parseInt(indexDigits, 16) & 0xFFFF;
                if (normalizedValue == hexFromIndexDigits) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return false;
    }

    /**
     * Tenta construir timestamp a partir de valores de DM com duas estratégias:
     * 1. Valores brutos como decimais
     * 2. Valores decodificados como BCD
     */
    public static LocalDateTime tryBuildManualTimestamp(List<Integer> valuesInOrder, int offset) {
        LocalDateTime candidatePrimary = tryBuildManualTimestampCandidate(
                valuesInOrder, offset + 2, offset + 3, offset + 4, offset + 5, offset + 6, offset + 7);
        LocalDateTime candidateSecondary = tryBuildManualTimestampCandidate(
                valuesInOrder, offset + 1, offset + 2, offset + 3, offset + 4, offset + 5, offset + 6);

        return pickBestManualTimestamp(candidatePrimary, candidateSecondary);
    }

    /**
     * Tenta construir timestamp a partir de índices específicos
     */
    private static LocalDateTime tryBuildManualTimestampCandidate(
            List<Integer> valuesInOrder,
            int yearIndex, int monthIndex, int dayIndex,
            int hourIndex, int minuteIndex, int secondIndex) {

        if (yearIndex < 0 || secondIndex >= valuesInOrder.size()) {
            return null;
        }

        int year = valuesInOrder.get(yearIndex).intValue();
        int month = valuesInOrder.get(monthIndex).intValue();
        int day = valuesInOrder.get(dayIndex).intValue();
        int hour = valuesInOrder.get(hourIndex).intValue();
        int minute = valuesInOrder.get(minuteIndex).intValue();
        int second = valuesInOrder.get(secondIndex).intValue();

        try {
            return buildManualTimestamp(year, month, day, hour, minute, second);
        } catch (Exception ignored) {
            // try BCD decoded values
        }

        int bcdYear = decodeBcdWord(year);
        int bcdMonth = decodeBcdWord(month);
        int bcdDay = decodeBcdWord(day);
        int bcdHour = decodeBcdWord(hour);
        int bcdMinute = decodeBcdWord(minute);
        int bcdSecond = decodeBcdWord(second);

        try {
            return buildManualTimestamp(bcdYear, bcdMonth, bcdDay, bcdHour, bcdMinute, bcdSecond);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Constrói LocalDateTime normalizando o ano
     */
    private static LocalDateTime buildManualTimestamp(int yearRaw, int month, int day,
            int hour, int minute, int second) {
        int year = normalizeManualYear(yearRaw);
        try {
            return LocalDateTime.of(year, month, day, hour, minute, second);
        } catch (Exception ex) {
            // Tenta invertendo dia e mês
            if (day >= 1 && day <= 12 && month > 12) {
                return LocalDateTime.of(year, day, month, hour, minute, second);
            }
            throw ex;
        }
    }

    /**
     * Escolhe o melhor timestamp entre dois candidatos
     */
    private static LocalDateTime pickBestManualTimestamp(LocalDateTime candidateA, LocalDateTime candidateB) {
        if (candidateA == null) {
            return candidateB;
        }
        if (candidateB == null) {
            return candidateA;
        }

        int scoreA = scoreManualTimestamp(candidateA);
        int scoreB = scoreManualTimestamp(candidateB);
        return scoreB > scoreA ? candidateB : candidateA;
    }

    /**
     * Calcula score de qualidade para um timestamp
     */
    private static int scoreManualTimestamp(LocalDateTime value) {
        int year = value.getYear();
        int score = 0;

        if (year >= 2020 && year <= 2100) {
            score += 1000;
        }

        score -= Math.abs(year - LocalDateTime.now().getYear());
        return score;
    }

    /**
     * Decodifica valor BCD (Binary Coded Decimal)
     */
    private static int decodeBcdWord(int raw) {
        int value = raw & 0xFFFF;
        int thousands = (value >> 12) & 0xF;
        int hundreds = (value >> 8) & 0xF;
        int tens = (value >> 4) & 0xF;
        int ones = value & 0xF;

        if (thousands > 9 || hundreds > 9 || tens > 9 || ones > 9) {
            return raw;
        }

        return (thousands * 1000) + (hundreds * 100) + (tens * 10) + ones;
    }

    /**
     * Normaliza ano para 4 dígitos
     */
    private static int normalizeManualYear(int yearRaw) {
        if (yearRaw >= 0 && yearRaw < 100) {
            return 2000 + yearRaw;
        }
        return yearRaw;
    }

    /**
     * Formata timestamp para string padrão
     */
    public static String formatTimestamp(LocalDateTime dt) {
        if (dt == null) {
            return "N/A";
        }
        return dt.format(MANUAL_DM_DATE_TIME_FMT);
    }

    /**
     * Formata timestamp para concatenação de 12 dígitos YYYYMMDDHHmmss
     */
    public static String formatTimestampConcatenated(LocalDateTime dt) {
        if (dt == null) {
            return "";
        }
        return String.format(
                "%04d%02d%02d%02d%02d%02d",
                Integer.valueOf(dt.getYear()),
                Integer.valueOf(dt.getMonthValue()),
                Integer.valueOf(dt.getDayOfMonth()),
                Integer.valueOf(dt.getHour()),
                Integer.valueOf(dt.getMinute()),
                Integer.valueOf(dt.getSecond()));
    }

    public static int getManualDmGroupSize() {
        return MANUAL_DM_GROUP_SIZE;
    }

    public static int getManualDmTagValuesOffset() {
        return MANUAL_DM_TAG_VALUES_OFFSET;
    }
}
