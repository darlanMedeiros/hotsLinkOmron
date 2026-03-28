interface QualityChartProps {
  percentage: number;
}

export function QualityChart({ percentage }: QualityChartProps) {
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

      
    </div>
  );
}
