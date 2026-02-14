import { Factory, Gauge } from 'lucide-react';
import { MetricCard } from './MetricCard';
import { QualityChart } from './QualityChart';
import { DefectsList, Defect } from './DefectsList';

interface ProductionLineProps {
  lineNumber: number;
  lineName: string;
  pecasPrensa: number;
  pecasRoller: number;
  aprovadas: number;
  rejeitadas: number;
  trendPrensa: number;
  trendRoller: number;
  defects: Defect[];
  color: string;
}

export function ProductionLine({
  lineNumber,
  lineName,
  pecasPrensa,
  pecasRoller,
  aprovadas,
  rejeitadas,
  trendPrensa,
  trendRoller,
  defects,
  color
}: ProductionLineProps) {
  const total = aprovadas + rejeitadas;
  const qualidadePercentual = total > 0 ? Math.round((aprovadas / total) * 100) : 0;

  return (
    <div className="bg-white rounded-lg shadow-lg border border-gray-200 overflow-hidden h-full flex flex-col">
      {/* Header da Linha */}
      <div className={`${color} px-4 py-3`}>
        <div className="flex items-center gap-2">
          <div className="bg-white/20 backdrop-blur-sm rounded p-1.5">
            <Factory className="w-4 h-4 text-white" />
          </div>
          <div>
            <h2 className="text-base font-bold text-white">MF 4{lineNumber}</h2>
            <p className="text-white/90 text-xs">{lineName}</p>
          </div>
        </div>
      </div>

      {/* Conteúdo */}
      <div className="p-4 flex-1 flex flex-col gap-4 overflow-y-auto">
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
          approved={aprovadas}
          rejected={rejeitadas}
        />

        {/* Lista de Defeitos */}
        <DefectsList defects={defects} />
      </div>
    </div>
  );
}