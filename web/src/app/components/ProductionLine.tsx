import { Factory, Gauge } from 'lucide-react';
import { MetricCard } from './MetricCard';
import { QualityChart } from './QualityChart';

interface ProductionLineProps {
  lineNumber: number;
  lineName: string;
  pecasPrensa: number;
  pecasRoller: number;
  qualidade: number;
  statusPrensaBit: number | null;
  lastUpdatedAt: string | null;
  trendPrensa: number;
  trendRoller: number;
  color: string;
}

export function ProductionLine({
  lineNumber,
  lineName,
  pecasPrensa,
  pecasRoller,
  qualidade,
  statusPrensaBit,
  lastUpdatedAt,
  trendPrensa,
  trendRoller,
  color
}: ProductionLineProps) {
  const qualidadePercentual = Math.max(0, Math.min(100, Math.round(qualidade)));
  const prensaStatusOn = statusPrensaBit === 1;
  const prensaStatusKnown = statusPrensaBit === 0 || statusPrensaBit === 1;

  return (
    <div className="bg-white rounded-lg shadow-lg border border-gray-200 overflow-hidden flex flex-col min-h-[320px]">
      <div className={`${color} px-4 py-3`}>
        <div className="flex items-center gap-2">
          <div className="bg-white/20 backdrop-blur-sm rounded p-1.5">
            <Factory className="w-4 h-4 text-white" />
          </div>
          <div>
            <h2 className="text-base font-bold text-white">{lineName}</h2>
            <p className="text-[11px] text-white/90">
              Ultima atualizacao: {lastUpdatedAt ?? '--'}
            </p>
          </div>
        </div>
      </div>

      <div className="p-4 flex-1 flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-3">
          <MetricCard
            title="Peças Prensa"
            value={pecasPrensa}
            unit="unidades"
            icon={Factory}
            trend={trendPrensa}
            color="bg-blue-500"
            footer={
              <div
                className={`inline-flex items-center gap-2 rounded-full border px-2.5 py-1 text-[11px] font-semibold ${
                  !prensaStatusKnown
                    ? 'border-slate-300 bg-slate-100 text-slate-600'
                    : prensaStatusOn
                      ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                      : 'border-slate-300 bg-slate-100 text-slate-600'
                }`}
              >
                <span
                  className={`h-2 w-2 rounded-full ${
                    !prensaStatusKnown
                      ? 'bg-slate-400'
                      : prensaStatusOn
                        ? 'bg-emerald-500 animate-pulse'
                        : 'bg-slate-500'
                  }`}
                />
                {prensaStatusKnown
                  ? (prensaStatusOn ? 'Ligado (bit=1)' : 'Desligado (bit=0)')
                  : 'Sem leitura do status'}
              </div>
            }
          />

          <MetricCard
            title="Peças na Roller"
            value={pecasRoller}
            unit="unidades"
            icon={Gauge}
            trend={trendRoller}
            color="bg-purple-500"
          />
        </div>

        <QualityChart
          percentage={qualidadePercentual}
        />
      </div>
    </div>
  );
}
