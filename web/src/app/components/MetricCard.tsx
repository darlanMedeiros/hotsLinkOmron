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
  containerClassName?: string;
  valueClassName?: string;
  titleClassName?: string;
  unitClassName?: string;
}

export function MetricCard({ title, value, unit, icon: Icon, trend, color, footer, containerClassName, valueClassName, titleClassName, unitClassName }: MetricCardProps) {
  return (
    <div className={`rounded-lg shadow-md p-4 border transition-colors duration-500 ${containerClassName || 'bg-white border-gray-200'}`}>
      <div className="flex items-start justify-between mb-3">
        {trend !== undefined ? null : null}
      </div>
      <div className="space-y-1">
        <h3 className={`text-xs font-medium ${titleClassName || 'text-gray-600'}`}>{title}</h3>
        <div className="flex items-baseline gap-1.5">
          <span className={`font-bold ${valueClassName || 'text-2xl text-gray-900'}`}>{value.toLocaleString('pt-BR')}</span>
          <span className={`text-xs ${unitClassName || 'text-gray-500'}`}>{unit}</span>
        </div>
        {footer ? <div className="pt-1">{footer}</div> : null}
      </div>
    </div>
  );
}
