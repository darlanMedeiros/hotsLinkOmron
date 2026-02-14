import { CheckCircle2, XCircle } from 'lucide-react';

interface QualityChartProps {
  percentage: number;
  approved: number;
  rejected: number;
}

export function QualityChart({ percentage, approved, rejected }: QualityChartProps) {
  const circumference = 2 * Math.PI * 40;
  const strokeDashoffset = circumference - (percentage / 100) * circumference;

  return (
    <div className="bg-white rounded-lg shadow-md p-4 border border-gray-200">
      <h3 className="text-sm font-semibold text-gray-900 mb-3">Qualidade</h3>
      
      <div className="flex flex-col items-center mb-4">
        <div className="relative w-24 h-24">
          <svg className="transform -rotate-90 w-24 h-24">
            <circle
              cx="48"
              cy="48"
              r="40"
              stroke="#e5e7eb"
              strokeWidth="8"
              fill="none"
            />
            <circle
              cx="48"
              cy="48"
              r="40"
              stroke={percentage >= 95 ? '#10b981' : percentage >= 85 ? '#f59e0b' : '#ef4444'}
              strokeWidth="8"
              fill="none"
              strokeDasharray={circumference}
              strokeDashoffset={strokeDashoffset}
              strokeLinecap="round"
            />
          </svg>
          <div className="absolute inset-0 flex items-center justify-center flex-col">
            <span className="text-2xl font-bold text-gray-900">{percentage}%</span>
            <span className="text-[10px] text-gray-500">Aprovação</span>
          </div>
        </div>
      </div>

      <div className="space-y-2">
        <div className="flex items-center justify-between p-2 bg-green-50 rounded-lg border border-green-200">
          <div className="flex items-center gap-1.5">
            <CheckCircle2 className="w-4 h-4 text-green-600" />
            <span className="text-xs font-medium text-gray-700">Aprovadas</span>
          </div>
          <span className="text-sm font-bold text-green-700">{approved.toLocaleString('pt-BR')}</span>
        </div>
        
        <div className="flex items-center justify-between p-2 bg-red-50 rounded-lg border border-red-200">
          <div className="flex items-center gap-1.5">
            <XCircle className="w-4 h-4 text-red-600" />
            <span className="text-xs font-medium text-gray-700">Rejeitadas</span>
          </div>
          <span className="text-sm font-bold text-red-700">{rejected.toLocaleString('pt-BR')}</span>
        </div>
      </div>
    </div>
  );
}