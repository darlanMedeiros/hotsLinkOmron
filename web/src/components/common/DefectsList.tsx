import { AlertCircle, Clock } from 'lucide-react';

export interface Defect {
  id: number;
  tipo: string;
  descricao: string;
  quantidade: number;
  timestamp: string;
  severidade: 'alta' | 'media' | 'baixa';
}

interface DefectsListProps {
  defects: Defect[];
}

export function DefectsList({ defects }: DefectsListProps) {
  const getSeverityColor = (severidade: string) => {
    switch (severidade) {
      case 'alta':
        return 'bg-red-100 text-red-700 border-red-300';
      case 'media':
        return 'bg-yellow-100 text-yellow-700 border-yellow-300';
      case 'baixa':
        return 'bg-blue-100 text-blue-700 border-blue-300';
      default:
        return 'bg-gray-100 text-gray-700 border-gray-300';
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-4 border border-gray-200">
      <div className="flex items-center gap-2 mb-3">
        <AlertCircle className="w-4 h-4 text-red-500" />
        <h3 className="text-sm font-semibold text-gray-900">Últimos Defeitos</h3>
      </div>
      <div className="space-y-2">
        {defects.map((defect) => (
          <div
            key={defect.id}
            className="border border-gray-200 rounded-lg p-3 hover:shadow-md transition-shadow"
          >
            <div className="flex items-start justify-between mb-1.5">
              <div className="flex-1">
                <div className="flex items-center gap-1.5 mb-1">
                  <h4 className="font-semibold text-gray-900 text-xs">{defect.tipo}</h4>
                  <span className={`text-[10px] px-1.5 py-0.5 rounded-full border ${getSeverityColor(defect.severidade)}`}>
                    {defect.severidade.toUpperCase()}
                  </span>
                </div>
                <p className="text-xs text-gray-600 mb-1.5 line-clamp-2">{defect.descricao}</p>
                <div className="flex items-center gap-3 text-[10px] text-gray-500">
                  <div className="flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    <span>{defect.timestamp}</span>
                  </div>
                  <span className="font-semibold">Qtd: {defect.quantidade}</span>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}