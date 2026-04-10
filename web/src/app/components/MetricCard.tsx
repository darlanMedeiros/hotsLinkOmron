import { ReactNode } from 'react';
import { LucideIcon } from 'lucide-react';

interface MetricCardProps {
  title: string;
  value: number;
  unit: string;
  icon: LucideIcon;
  trend?: number;
  color: string;
  footer?: ReactNode;
}

export function MetricCard({ title, value, unit, icon: Icon, trend, color, footer }: MetricCardProps) {
  return (
    <div className="bg-white rounded-lg shadow-md p-4 border border-gray-200">
      <div className="flex items-start justify-between mb-3">
        {trend !== undefined ? null : null}
      </div>
      <div className="space-y-1">
        <h3 className="text-gray-600 text-xs font-medium">{title}</h3>
        <div className="flex items-baseline gap-1.5">
          <span className="text-2xl font-bold text-gray-900">{value.toLocaleString('pt-BR')}</span>
          <span className="text-gray-500 text-xs">{unit}</span>
        </div>
        {footer ? <div className="pt-1">{footer}</div> : null}
      </div>
    </div>
  );
}
