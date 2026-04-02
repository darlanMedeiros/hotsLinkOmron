import { useEffect, useMemo, useState } from 'react';
import { Activity, Play, Square, Terminal, AlertTriangle, Loader2 } from 'lucide-react';
import { requestApi } from '../../services/api';
import { Button } from '../components/ui/button';

interface CollectorStatus {
  running: boolean;
  pid: number | null;
  exitCode: number | null;
  startedAt: string | null;
  stoppedAt: string | null;
  command: string | null;
  workingDirectory: string | null;
  lastError: string | null;
  recentLogs: string[];
}

const STATUS_POLL_MS = 3000;

const defaultStatus: CollectorStatus = {
  running: false,
  pid: null,
  exitCode: null,
  startedAt: null,
  stoppedAt: null,
  command: null,
  workingDirectory: null,
  lastError: null,
  recentLogs: [],
};

function formatDate(value: string | null): string {
  if (!value) {
    return '-';
  }

  const dt = new Date(value);
  if (Number.isNaN(dt.getTime())) {
    return value;
  }

  return dt.toLocaleString('pt-BR');
}

export function CollectorControlPage() {
  const [status, setStatus] = useState<CollectorStatus>(defaultStatus);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const logs = useMemo(() => {
    return status.recentLogs.slice(-40);
  }, [status.recentLogs]);

  const loadStatus = async () => {
    try {
      const data = await requestApi<CollectorStatus>('/api/collector/status');
      setStatus(data);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao consultar status do collector');
    } finally {
      setIsLoading(false);
    }
  };

  const handleAction = async (action: 'start' | 'stop') => {
    setIsSubmitting(true);
    setError(null);
    try {
      const data = await requestApi<CollectorStatus>(`/api/collector/${action}`, { method: 'POST' });
      setStatus(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : `Erro ao ${action === 'start' ? 'iniciar' : 'parar'} collector`);
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    let active = true;
    let timer: number | null = null;

    const run = async () => {
      if (!active) {
        return;
      }
      await loadStatus();
      if (!active) {
        return;
      }
      timer = window.setTimeout(run, STATUS_POLL_MS);
    };

    run();

    return () => {
      active = false;
      if (timer !== null) {
        window.clearTimeout(timer);
      }
    };
  }, []);

  return (
    <div className="space-y-4">
      <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Controle Collector Multi PLC</h2>
            <p className="text-sm text-slate-500">Inicie ou pare o processo local do collector diretamente pela interface web.</p>
          </div>

          <div className="flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            {status.running ? (
              <Activity className="h-4 w-4 text-emerald-500 animate-pulse" />
            ) : (
              <AlertTriangle className="h-4 w-4 text-amber-500" />
            )}
            <span className="text-sm font-medium text-slate-700">
              {isLoading ? 'Carregando status...' : status.running ? 'Collector em execucao' : 'Collector parado'}
            </span>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
          <InfoCard label="PID" value={status.pid ? String(status.pid) : '-'} />
          <InfoCard label="Inicio" value={formatDate(status.startedAt)} />
          <InfoCard label="Parada" value={formatDate(status.stoppedAt)} />
          <InfoCard label="Exit code" value={status.exitCode === null ? '-' : String(status.exitCode)} />
        </div>

        <div className="mt-4 flex flex-wrap gap-2">
          <Button onClick={() => handleAction('start')} disabled={status.running || isSubmitting}>
            {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Play className="h-4 w-4" />}
            Iniciar Collector
          </Button>
          <Button variant="destructive" onClick={() => handleAction('stop')} disabled={!status.running || isSubmitting}>
            {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <Square className="h-4 w-4" />}
            Parar Collector
          </Button>
        </div>

        {status.command && (
          <p className="mt-3 text-xs text-slate-500">
            <strong>Comando:</strong> {status.command}
          </p>
        )}
        {status.workingDirectory && (
          <p className="mt-1 text-xs text-slate-500">
            <strong>Diretorio:</strong> {status.workingDirectory}
          </p>
        )}

        {(error || status.lastError) && (
          <div className="mt-4 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error || status.lastError}
          </div>
        )}
      </section>

      <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-200 px-4 py-3">
          <h3 className="flex items-center gap-2 text-sm font-semibold text-slate-900">
            <Terminal className="h-4 w-4" />
            Logs recentes do collector
          </h3>
        </div>
        <div className="max-h-[380px] overflow-auto bg-slate-950 px-4 py-3 font-mono text-xs text-slate-100">
          {logs.length === 0 ? (
            <p className="text-slate-400">Nenhum log capturado ate o momento.</p>
          ) : (
            logs.map((line, index) => (
              <p key={`${index}-${line}`} className="whitespace-pre-wrap break-words">
                {line}
              </p>
            ))
          )}
        </div>
      </section>
    </div>
  );
}

interface InfoCardProps {
  label: string;
  value: string;
}

function InfoCard({ label, value }: InfoCardProps) {
  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
      <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
      <p className="text-sm font-semibold text-slate-900">{value}</p>
    </div>
  );
}
