import { Factory, Gauge } from 'lucide-react';
import { MetricCard } from './MetricCard';
import { QualityChart } from './QualityChart';

interface ProductionLineProps {
  lineNumber: number;
  lineName: string;
  pecasPrensa: number;
  pecasRoller: number;
  qualidade: number;
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
  lastUpdatedAt,
  trendPrensa,
  trendRoller,
  color
}: ProductionLineProps) {
  const qualidadePercentual = Math.max(0, Math.min(100, Math.round(qualidade)));

  return (
    <div className="bg-white rounded-lg shadow-lg border border-gray-200 overflow-hidden flex flex-col min-h-[320px]">
      {/* Header da Linha */}
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

      {/* Conteúdo */}
      <div className="p-4 flex-1 flex flex-col gap-4">
        {/* Métricas - Prensa e Roller lado a lado */}
        <div className="grid grid-cols-2 gap-3">
          <MetricCard
            title="Peças Prensa"
            value={pecasPrensa}
            unit="unidades"
            icon={Factory}
            trend={trendPrensa}
            color="bg-blue-500"
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
        
        {/* Qualidade */}
        <QualityChart
          percentage={qualidadePercentual}
        />
      </div>
    </div>
  );
}
