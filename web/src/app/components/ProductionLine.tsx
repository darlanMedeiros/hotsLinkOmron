import { Factory, Gauge, Package, ThermometerSun } from 'lucide-react';
import { MetricCard } from './MetricCard';
import { QualityChart } from './QualityChart';
import { useLatestQuality } from '../../hooks/useLatestQuality';

interface ProductionLineProps {
  lineNumber: number;
  lineName: string;
  machineId: number;
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
  machineId,
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

  const { data, isLoading } = useLatestQuality(machineId);

  // Mock data and parsing
  let previa = 0;
  let defeitos: { name: string; percentage: number }[] = [];
  let estoque = 0;
  let forno18 = 0;

  if (machineId === 13) {
    previa = Math.round(data?.qualidadeParcial ?? 0);
    estoque = 8; // Placeholder
    forno18 = 879; // Placeholder
    defeitos = data?.defeitos
      ? [...data.defeitos]
          .sort((a, b) => b.value - a.value)
          .slice(0, 3)
          .map(d => ({
            name: d.defeitoName,
            percentage: (d.value / (data.value || 1)) * 100
          }))
      : [];
  } else if (machineId === 42) {
    previa = 96;
    estoque = 8;
    forno18 = 879;
    defeitos = [
      { name: "BOLHA DE AR", percentage: 1.3 },
      { name: "PONTA QUEBRADA", percentage: 0.8 },
      { name: "GRUMO", percentage: 0.7 }
    ];
  } else if (machineId === 43) {
    previa = 98;
    estoque = 12;
    forno18 = 912;
    defeitos = [
      { name: "SALIENCIA", percentage: 0.9 },
      { name: "RISCO", percentage: 0.5 },
      { name: "MANCHA", percentage: 0.4 }
    ];
  }

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

          <MetricCard
            title="Estoque"
            value={estoque}
            unit="unidades"
            icon={Package}
            trend={0}
            color="bg-slate-500"
          />

          <MetricCard
            title="Forno 18"
            value={forno18}
            unit="m²"
            icon={ThermometerSun}
            trend={0}
            color="bg-orange-500"
          />
        </div>

        <div className="p-3 border border-slate-200 rounded-lg bg-slate-50 flex-1 flex flex-col justify-center relative">
          {isLoading && machineId === 13 && (
            <div className="absolute inset-0 bg-white/50 backdrop-blur-sm z-10 flex items-center justify-center rounded-lg">
              <div className="animate-pulse flex gap-1">
                <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
              </div>
            </div>
          )}
          
          <h3 className="text-[10px] font-bold text-slate-500 uppercase mb-2 tracking-wider">Qualidade</h3>
          
          <div className="flex justify-around items-center mb-4">
            <div className="flex flex-col items-center justify-center">
              <div className="transform scale-90 origin-bottom">
                <QualityChart percentage={previa} />
              </div>
              <div className="text-center mt-1 text-[10px] font-bold text-slate-500 uppercase tracking-wider">Prévia</div>
            </div>
            
            <div className="flex flex-col items-center justify-center">
              <div className="transform scale-90 origin-bottom">
                <QualityChart percentage={qualidadePercentual} />
              </div>
              <div className="text-center mt-1 text-[10px] font-bold text-slate-500 uppercase tracking-wider">Máquina</div>
            </div>
          </div>
          
          <span className="text-[9px] font-semibold text-slate-500 mb-1 block uppercase tracking-wider">Principais Defeitos</span>
          <div className="space-y-1">
            {defeitos.map((def, idx) => (
              <div key={idx} className="flex justify-between items-center bg-white border border-slate-200 px-2 py-1.5 rounded shadow-sm text-[10px]">
                <span className="font-semibold text-slate-600 uppercase truncate">{def.name}</span>
                <span className="text-slate-500 font-bold">{def.percentage.toFixed(1).replace('.', ',')}%</span>
              </div>
            ))}
            {defeitos.length === 0 && (
              <div className="text-center text-[10px] text-slate-400 italic py-1">Nenhum defeito</div>
            )}
          </div>
          
          {(machineId === 42 || machineId === 43) && (
            <div className="absolute top-2 right-2 text-[8px] text-amber-600 bg-amber-50 border border-amber-200 px-1.5 py-0.5 rounded-full italic">
              Mock
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
