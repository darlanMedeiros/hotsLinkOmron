import { useEffect, useState } from 'react';
import { Factory, Activity, AlertTriangle } from 'lucide-react';
import { ProductionLine } from './components/ProductionLine';
import { AdminCrudScreen } from './components/AdminCrudScreen';
import { PlcTagCrudScreen } from './components/PlcTagCrudScreen';

interface TagValueResponse {
  tagName: string;
  memoryName: string;
  deviceMnemonic: string;
  value: number | null;
  updatedAt: string | null;
}

interface LineData {
  pecasPrensa: number;
  pecasRoller: number;
  qualidade: number;
  lastUpdatedAt: string | null;
}

interface LineConfig {
  key: '41' | '42' | '43';
  lineNumber: number;
  lineName: string;
  color: string;
  tags: {
    pecasPrensa: string[];
    pecasRoller: string[];
    qualidade: string[];
  };
}

const LINE_CONFIG: LineConfig[] = [
  {
    key: '41',
    lineNumber: 1,
    lineName: 'MF 41',
    color: 'bg-gradient-to-r from-blue-500 to-blue-600',
    tags: {
      pecasPrensa: ['PECAPH29'],
      pecasRoller: ['PECAROLLERCARGA41'],
      qualidade: ['QUALIDADE41'],
    },
  },
  {
    key: '42',
    lineNumber: 2,
    lineName: 'MF 42',
    color: 'bg-gradient-to-r from-green-500 to-green-600',
    tags: {
      pecasPrensa: ['PECAPH30'],
      pecasRoller: ['PECAROLLERCARGA42'],
      qualidade: ['QUALIDADE42'],
    },
  },
  {
    key: '43',
    lineNumber: 3,
    lineName: 'MF 43',
    color: 'bg-gradient-to-r from-orange-500 to-orange-600',
    tags: {
      pecasPrensa: ['PECAPH31'],
      pecasRoller: ['PECAROLLERCARGA43'],
      qualidade: ['QUALIDADE43'],
    },
  },
];

const DEFAULT_LINE_DATA: LineData = {
  pecasPrensa: 0,
  pecasRoller: 0,
  qualidade: 0,
  lastUpdatedAt: null,
};

const POLL_INTERVAL_OK_MS = 5000;
const POLL_INTERVAL_ERROR_START_MS = 10000;
const POLL_INTERVAL_ERROR_MAX_MS = 60000;
const BACKEND_TARGET = import.meta.env.VITE_API_URL || 'http://localhost:8080';
const OFFLINE_PROBE_TAG = 'PECAPH29';

export default function App() {
  const [viewMode, setViewMode] = useState<'dashboard' | 'cadastros' | 'plc-tag'>('dashboard');
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
    if (viewMode !== 'dashboard') {
      return;
    }

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
          const [pecasPrensaTag, pecasRollerTag, qualidadeTag] = await Promise.all([
            fetchTagValue(line.tags.pecasPrensa),
            fetchTagValue(line.tags.pecasRoller),
            fetchTagValue(line.tags.qualidade),
          ]);

          const lastUpdatedAt = mostRecent([
            pecasPrensaTag.updatedAt,
            pecasRollerTag.updatedAt,
            qualidadeTag.updatedAt,
          ]);

          nextData[line.key] = {
            pecasPrensa: pecasPrensaTag.value ?? 0,
            pecasRoller: pecasRollerTag.value ?? 0,
            qualidade: qualidadeTag.value ?? 0,
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
  }, [viewMode]);

  return (
    <div className="min-h-screen lg:h-screen bg-gradient-to-br from-gray-50 to-gray-100 flex flex-col overflow-x-hidden">
      <header className="bg-gradient-to-r from-blue-600 to-blue-800 shadow-xl">
        <div className="px-4 sm:px-6 py-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <Factory className="w-7 h-7 text-white" />
              <div>
                <h1 className="text-xl font-bold text-white">Dashboard PB 4</h1>
                <p className="text-blue-100 text-xs">
                  {viewMode === 'dashboard'
                    ? 'Monitoramento em tempo real - 3 linhas'
                    : viewMode === 'cadastros'
                      ? 'Cadastro e manutencao de estrutura'
                      : 'Cadastro PLC, memory e tags'}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <div className="flex items-center rounded-lg border border-white/20 bg-white/10 p-1">
                <button
                  onClick={() => setViewMode('dashboard')}
                  className={`rounded-md px-3 py-1.5 text-sm font-medium transition ${
                    viewMode === 'dashboard'
                      ? 'bg-white text-blue-700'
                      : 'text-white hover:bg-white/10'
                  }`}
                >
                  Dashboard
                </button>
                <button
                  onClick={() => setViewMode('cadastros')}
                  className={`rounded-md px-3 py-1.5 text-sm font-medium transition ${
                    viewMode === 'cadastros'
                      ? 'bg-white text-blue-700'
                      : 'text-white hover:bg-white/10'
                  }`}
                >
                  Cadastros
                </button>
                <button
                  onClick={() => setViewMode('plc-tag')}
                  className={`rounded-md px-3 py-1.5 text-sm font-medium transition ${
                    viewMode === 'plc-tag'
                      ? 'bg-white text-blue-700'
                      : 'text-white hover:bg-white/10'
                  }`}
                >
                  PLC/TAG
                </button>
              </div>
              <div className="flex items-center gap-2 bg-white/10 backdrop-blur-sm px-3 py-1.5 rounded-lg">
                {isBackendOffline ? (
                  <AlertTriangle className="w-4 h-4 text-red-300" />
                ) : (
                  <Activity className="w-4 h-4 text-green-300 animate-pulse" />
                )}
                <span className="text-white text-sm font-medium">
                  {isLoading ? 'Carregando' : isBackendOffline ? 'Servidor Offline' : 'Sistema Ativo'}
                </span>
              </div>
            </div>
          </div>
        </div>
      </header>

      <main className="flex-1 overflow-y-auto p-3 sm:p-4">
        {viewMode === 'dashboard' && error && (
          <div className="mb-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 flex items-center gap-2">
            <AlertTriangle className="h-4 w-4" />
            <span>{error}</span>
          </div>
        )}
        {viewMode === 'dashboard' && isBackendOffline && (
          <div className="mb-3 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
            Backend esperado: <strong>{BACKEND_TARGET}</strong>
          </div>
        )}

        {viewMode === 'dashboard' ? (
          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3 sm:gap-4">
            {LINE_CONFIG.map((line) => {
              const data = linesData[line.key] ?? DEFAULT_LINE_DATA;
              return (
                <ProductionLine
                  key={line.key}
                  lineNumber={line.lineNumber}
                  lineName={line.lineName}
                  pecasPrensa={data.pecasPrensa}
                  pecasRoller={data.pecasRoller}
                  qualidade={data.qualidade}
                  lastUpdatedAt={data.lastUpdatedAt}
                  trendPrensa={0}
                  trendRoller={0}
                  color={line.color}
                />
              );
            })}
          </div>
        ) : viewMode === 'cadastros' ? (
          <AdminCrudScreen />
        ) : (
          <PlcTagCrudScreen />
        )}
      </main>
    </div>
  );
}
