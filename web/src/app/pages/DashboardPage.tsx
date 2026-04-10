import { Activity, AlertTriangle } from 'lucide-react';
import { ProductionLine } from '../components/ProductionLine';
import { useDashboardPolling, LINE_CONFIG, DEFAULT_LINE_DATA, BACKEND_TARGET } from '../../hooks/useDashboardPolling';

export function DashboardPage() {
  const { linesData, isLoading, error, isBackendOffline } = useDashboardPolling();

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between rounded-lg border border-slate-200 bg-white p-3 shadow-sm">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Dashboard</h2>
          <p className="text-sm text-slate-500">Monitoramento das linhas em tempo real.</p>
        </div>
        <div className="flex items-center gap-2 bg-slate-100 px-3 py-1.5 rounded-lg border border-slate-200">
          {isBackendOffline ? (
            <AlertTriangle className="w-4 h-4 text-red-500" />
          ) : (
            <Activity className="w-4 h-4 text-emerald-500 animate-pulse" />
          )}
          <span className="text-sm font-medium text-slate-700">
            {isLoading ? 'Carregando' : isBackendOffline ? 'Servidor Offline' : 'Sistema Ativo'}
          </span>
        </div>
      </div>

      {error && (
        <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 flex items-center gap-2 shadow-sm">
          <AlertTriangle className="h-4 w-4" />
          <span>{error}</span>
        </div>
      )}
      {isBackendOffline && (
        <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800 shadow-sm">
          Backend esperado: <strong>{BACKEND_TARGET}</strong>
        </div>
      )}

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
              statusPrensaBit={data.statusPrensaBit}
              lastUpdatedAt={data.lastUpdatedAt}
              trendPrensa={0}
              trendRoller={0}
              color={line.color}
            />
          );
        })}
      </div>
    </div>
  );
}
