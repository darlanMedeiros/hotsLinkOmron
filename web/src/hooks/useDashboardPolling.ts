import { useEffect, useState } from 'react';

export interface TagValueResponse {
  tagName: string;
  memoryName: string;
  deviceMnemonic: string;
  value: number | null;
  updatedAt: string | null;
}

export interface LineData {
  pecasPrensa: number;
  pecasRoller: number;
  qualidade: number;
  statusPrensaBit: number | null;
  lastUpdatedAt: string | null;
}

export interface LineConfig {
  key: '41' | '42' | '43';
  lineNumber: number;
  lineName: string;
  machineId: number;
  color: string;
  tags: {
    pecasPrensa: string[];
    pecasRoller: string[];
    qualidade: string[];
    statusPrensa: string[];
  };
}

export const LINE_CONFIG: LineConfig[] = [
  {
    key: '41',
    lineNumber: 1,
    lineName: 'MF 41',
    machineId: 13, // ID do banco para ESCOLHA 41
    color: 'bg-gradient-to-r from-blue-500 to-blue-600',
    tags: {
      pecasPrensa: ['PECAPH29'],
      pecasRoller: ['PECAROLLERCARGA41'],
      qualidade: ['Qualidade_Maquina_Current'],
      statusPrensa: ['STATUS_PH29'],
    },
  },
  {
    key: '42',
    lineNumber: 2,
    lineName: 'MF 42',
    machineId: 42, // Mock ID
    color: 'bg-gradient-to-r from-green-500 to-green-600',
    tags: {
      pecasPrensa: ['PECAPH30'],
      pecasRoller: ['PECAROLLERCARGA42'],
      qualidade: ['QUALIDADE42'],
      statusPrensa: ['STATUS_PH30'],
    },
  },
  {
    key: '43',
    lineNumber: 3,
    lineName: 'MF 43',
    machineId: 43, // Mock ID
    color: 'bg-gradient-to-r from-orange-500 to-orange-600',
    tags: {
      pecasPrensa: ['PECAPH31'],
      pecasRoller: ['PECAROLLERCARGA43'],
      qualidade: ['QUALIDADE43'],
      statusPrensa: ['STATUS_PH31'],
    },
  },
];

export const DEFAULT_LINE_DATA: LineData = {
  pecasPrensa: 0,
  pecasRoller: 0,
  qualidade: 0,
  statusPrensaBit: null,
  lastUpdatedAt: null,
};

const POLL_INTERVAL_OK_MS = 5000;
const POLL_INTERVAL_ERROR_START_MS = 10000;
const POLL_INTERVAL_ERROR_MAX_MS = 60000;
export const BACKEND_TARGET = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const OFFLINE_PROBE_TAG = 'PECAPH29';

export function useDashboardPolling() {
  const [linesData, setLinesData] = useState<Record<string, LineData>>(() => {
    return LINE_CONFIG.reduce<Record<string, LineData>>((acc, line) => {
      acc[line.key] = { ...DEFAULT_LINE_DATA };
      return acc;
    }, {});
  });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isBackendOffline, setIsBackendOffline] = useState(false);

  useEffect(() => {
    let active = true;
    let timer: number | null = null;
    let nextDelayMs = POLL_INTERVAL_OK_MS;
    let backendOffline = false;

    const fetchTagValue = async (tagNames: string[]): Promise<TagValueResponse> => {
      const pickMostRecent = (items: TagValueResponse[]): TagValueResponse => {
        const sorted = [...items].sort((a, b) => {
          const aTime = a.updatedAt ? Date.parse(a.updatedAt) : 0;
          const bTime = b.updatedAt ? Date.parse(b.updatedAt) : 0;
          return bTime - aTime;
        });
        return sorted[0];
      };

      let lastStatus = 0;
      for (const tagName of tagNames) {
        const response = await fetch(`/api/devices/tag/${encodeURIComponent(tagName)}`);
        if (response.ok) {
          const body = (await response.json()) as TagValueResponse | TagValueResponse[];
          if (Array.isArray(body)) {
            if (body.length === 0) {
              continue;
            }
            return pickMostRecent(body);
          }
          return body;
        }
        lastStatus = response.status;
        if (response.status !== 404) {
          throw new Error(`Falha ao ler TAG ${tagName} (HTTP ${response.status})`);
        }
      }
      throw new Error(`Falha ao ler TAG ${tagNames[0]} (HTTP ${lastStatus || 404})`);
    };

    const formatTimestamp = (raw: string | null): string | null => {
      if (!raw) {
        return null;
      }
      const dt = new Date(raw);
      if (Number.isNaN(dt.getTime())) {
        return raw;
      }
      return dt.toLocaleString('pt-BR');
    };

    const mostRecent = (values: Array<string | null>): string | null => {
      let best: Date | null = null;
      for (const raw of values) {
        if (!raw) {
          continue;
        }
        const parsed = new Date(raw);
        if (Number.isNaN(parsed.getTime())) {
          continue;
        }
        if (best === null || parsed.getTime() > best.getTime()) {
          best = parsed;
        }
      }
      return best ? best.toISOString() : null;
    };

    const probeBackend = async (): Promise<boolean> => {
      try {
        const response = await fetch(`/api/devices/tag/${encodeURIComponent(OFFLINE_PROBE_TAG)}`);
        return response.ok || response.status >= 400;
      } catch (err) {
        return !(err instanceof TypeError);
      }
    };

    const loadAll = async (): Promise<boolean> => {
      try {
        if (backendOffline) {
          const recovered = await probeBackend();
          if (!recovered) {
            setError(`Falha de conexao: servidor ${BACKEND_TARGET} nao esta ativo.`);
            setIsBackendOffline(true);
            return false;
          }
          backendOffline = false;
          setIsBackendOffline(false);
        }

        const nextData: Record<string, LineData> = {};

        for (const line of LINE_CONFIG) {
          const [pecasPrensaTag, pecasRollerTag, qualidadeTag, statusPrensaTag] = await Promise.all([
            fetchTagValue(line.tags.pecasPrensa),
            fetchTagValue(line.tags.pecasRoller),
            fetchTagValue(line.tags.qualidade),
            fetchTagValue(line.tags.statusPrensa),
          ]);

          const lastUpdatedAt = mostRecent([
            pecasPrensaTag.updatedAt,
            pecasRollerTag.updatedAt,
            qualidadeTag.updatedAt,
            statusPrensaTag.updatedAt,
          ]);

          nextData[line.key] = {
            pecasPrensa: pecasPrensaTag.value ?? 0,
            pecasRoller: pecasRollerTag.value ?? 0,
            qualidade: qualidadeTag.value ?? 0,
            statusPrensaBit: statusPrensaTag.value,
            lastUpdatedAt: formatTimestamp(lastUpdatedAt),
          };
        }

        if (!active) {
          return false;
        }
        setLinesData(nextData);
        setError(null);
        setIsBackendOffline(false);
        return true;
      } catch (err) {
        if (!active) {
          return false;
        }
        const offline = err instanceof TypeError;
        backendOffline = offline;
        const message =
          offline
            ? `Falha de conexao: servidor ${BACKEND_TARGET} nao esta ativo.`
            : err instanceof Error
              ? err.message
              : 'Erro desconhecido ao consultar API';
        setIsBackendOffline(offline);
        setError(message);
        return false;
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    };

    const scheduleNext = (delayMs: number) => {
      if (!active) {
        return;
      }
      timer = window.setTimeout(runPolling, delayMs);
    };

    const runPolling = async () => {
      const ok = await loadAll();
      if (!active) {
        return;
      }

      if (ok) {
        nextDelayMs = POLL_INTERVAL_OK_MS;
      } else if (nextDelayMs < POLL_INTERVAL_ERROR_START_MS) {
        nextDelayMs = POLL_INTERVAL_ERROR_START_MS;
      } else {
        nextDelayMs = Math.min(nextDelayMs * 2, POLL_INTERVAL_ERROR_MAX_MS);
      }

      scheduleNext(nextDelayMs);
    };

    runPolling();

    return () => {
      active = false;
      if (timer !== null) {
        window.clearTimeout(timer);
      }
    };
  }, []);

  return { linesData, isLoading, error, isBackendOffline };
}
