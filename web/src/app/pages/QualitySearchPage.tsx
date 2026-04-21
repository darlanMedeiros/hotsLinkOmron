import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { AlertTriangle, CheckCircle, Clock, Download, Filter, Search, ShieldCheck } from 'lucide-react';
import { requestApi } from '../../services/api';

interface Machine {
  id: number;
  name: string;
}

interface Turno {
  id: number;
  name: string;
  horaInicio: string;
  horaFinal: string;
}

interface QualidadeDefeito {
  defeitoId: number;
  defeitoName: string;
  value: number;
  amostragem: number;
}

interface QualidadeHistory {
  id: number;
  machineId: number;
  machineName: string;
  value: number;
  hora: string;
  turnoId: number;
  turnoName: string;
  qualidadeParcial: number;
  defeitos: QualidadeDefeito[];
}

export const QualitySearchPage: React.FC = () => {
  const toLocalInputDate = (date: Date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };
  const today = new Date();
  const sevenDaysAgo = new Date(today);
  sevenDaysAgo.setDate(today.getDate() - 7);

  const parseApiDateTime = (value: string): Date | null => {
    if (!value) return null;

    const direct = new Date(value);
    if (!Number.isNaN(direct.getTime())) {
      return direct;
    }

    const normalized = value.includes(' ') ? value.replace(' ', 'T') : value;
    const fallback = new Date(normalized);
    if (!Number.isNaN(fallback.getTime())) {
      return fallback;
    }

    return null;
  };

  const formatApiDateTime = (value: string): string => {
    const parsed = parseApiDateTime(value);
    return parsed ? parsed.toLocaleString('pt-BR') : value;
  };

  const [machines, setMachines] = useState<Machine[]>([]);
  const [turnos, setTurnos] = useState<Turno[]>([]);
  
  const [selectedMachineId, setSelectedMachineId] = useState<string>('');
  const [selectedTurnoId, setSelectedTurnoId] = useState<string>('');
  const [selectedStartDate, setSelectedStartDate] = useState<string>(toLocalInputDate(sevenDaysAgo));
  const [selectedEndDate, setSelectedEndDate] = useState<string>(toLocalInputDate(today));
  
  const [history, setHistory] = useState<QualidadeHistory[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hasSearched, setHasSearched] = useState(false);

  useEffect(() => {
    const loadFilters = async () => {
      try {
        const [machineData, turnoData] = await Promise.all([
          requestApi<Machine[]>('/api/machines'),
          requestApi<Turno[]>('/api/turnos'),
        ]);
        setMachines(machineData);
        setTurnos(turnoData);
      } catch (err) {
        setError(`Erro ao carregar filtros: ${err instanceof Error ? err.message : 'erro desconhecido'}`);
      }
    };
    loadFilters();
  }, []);

  const handleSearch = useCallback(async () => {
    setLoading(true);
    setError(null);
    setHasSearched(true);
    try {
      const params = new URLSearchParams();
      if (selectedMachineId) params.set('machineId', selectedMachineId);
      if (selectedTurnoId) params.set('turnoId', selectedTurnoId);
      if (selectedStartDate) params.set('startDate', selectedStartDate);
      if (selectedEndDate) params.set('endDate', selectedEndDate);

      const data = await requestApi<QualidadeHistory[]>(`/api/qualidade/historico?${params.toString()}`);
      setHistory(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao buscar historico de qualidade.');
    } finally {
      setLoading(false);
    }
  }, [selectedMachineId, selectedTurnoId, selectedStartDate, selectedEndDate]);

  // Totais e Métricas
  const metrics = useMemo(() => {
    if (history.length === 0) return { totalRecords: 0, avgQuality: 0, totalDefects: 0 };
    
    const totalRecords = history.length;
    const avgQuality = history.reduce((acc, curr) => acc + curr.qualidadeParcial, 0) / totalRecords;
    const totalDefects = history.reduce((acc, curr) => 
      acc + curr.defeitos.reduce((dAcc, dCurr) => dAcc + dCurr.value, 0), 0);
    
    return { totalRecords, avgQuality, totalDefects };
  }, [history]);

  const defectSummary = useMemo(() => {
    const map = new Map<string, number>();
    history.forEach(reg => {
      reg.defeitos.forEach(def => {
        map.set(def.defeitoName, (map.get(def.defeitoName) || 0) + def.value);
      });
    });
    return Array.from(map.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 10);
  }, [history]);

  const handleExportCsv = () => {
    if (history.length === 0) return;

    const headers = ['Data/Hora', 'Maquina', 'Turno', 'Amostragem', 'Qualidade (%)', 'Defeitos'];
    const rows = history.map(h => [
      formatApiDateTime(h.hora),
      h.machineName,
      h.turnoName,
      h.value,
      h.qualidadeParcial.toFixed(1),
      h.defeitos.map(d => `${d.defeitoName}: ${d.value}`).join(' | ')
    ]);

    const csvContent = "\uFEFF" + [
      headers.join(';'),
      ...rows.map(row => row.map(v => `"${String(v).replace(/"/g, '""')}"`).join(';'))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `relatorio-qualidade-${selectedStartDate}.csv`;
    link.click();
  };

  return (
    <div className="space-y-4">
      {/* Header Section */}
      <section className="rounded-lg border border-slate-200 bg-white px-4 py-3 shadow-sm">
        <div className="flex items-center gap-2">
          <ShieldCheck className="h-5 w-5 text-blue-600" />
          <h2 className="text-lg font-semibold text-slate-900">Pesquisa de Qualidade</h2>
        </div>
        <p className="mt-1 text-sm text-slate-600">Relatórios de amostragem e defeitos por máquina e turno.</p>
      </section>

      {/* Filters Section */}
      <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex items-center gap-2 mb-4 text-slate-700 font-medium text-sm">
          <Filter className="w-4 h-4" />
          <span>Filtros de Pesquisa</span>
        </div>
        
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500 uppercase tracking-wider">Máquina</label>
            <select
              value={selectedMachineId}
              onChange={(e) => setSelectedMachineId(e.target.value)}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition"
            >
              <option value="">Todas as Máquinas</option>
              {machines.map(m => (
                <option key={m.id} value={m.id}>{m.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500 uppercase tracking-wider">Turno</label>
            <select
              value={selectedTurnoId}
              onChange={(e) => setSelectedTurnoId(e.target.value)}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition"
            >
              <option value="">Todos os Turnos</option>
              {turnos.map(t => (
                <option key={t.id} value={t.id}>{t.name} ({t.horaInicio} - {t.horaFinal})</option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500 uppercase tracking-wider">Data Início</label>
            <input
              type="date"
              value={selectedStartDate}
              onChange={(e) => setSelectedStartDate(e.target.value)}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition"
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-medium text-slate-500 uppercase tracking-wider">Data Fim</label>
            <input
              type="date"
              value={selectedEndDate}
              onChange={(e) => setSelectedEndDate(e.target.value)}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition"
            />
          </div>
        </div>

        <div className="mt-4 flex justify-end">
          <button
            onClick={handleSearch}
            disabled={loading}
            className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-6 py-2 text-sm font-medium text-white transition hover:bg-blue-700 shadow-sm disabled:bg-blue-300"
          >
            <Search className="h-4 w-4" />
            {loading ? 'Pesquisando...' : 'Pesquisar'}
          </button>
        </div>
      </section>

      {/* Metrics Cards */}
      {history.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-white p-4 rounded-lg border border-slate-200 shadow-sm flex items-center gap-4">
            <div className="bg-blue-100 p-3 rounded-full text-blue-600">
              <Clock className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs text-slate-500 font-medium uppercase">Registros</p>
              <p className="text-2xl font-bold text-slate-900">{metrics.totalRecords}</p>
            </div>
          </div>
          
          <div className="bg-white p-4 rounded-lg border border-slate-200 shadow-sm flex items-center gap-4">
            <div className="bg-emerald-100 p-3 rounded-full text-emerald-600">
              <CheckCircle className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs text-slate-500 font-medium uppercase">Qualidade Média</p>
              <p className="text-2xl font-bold text-slate-900">{metrics.avgQuality.toFixed(1)}%</p>
            </div>
          </div>

          <div className="bg-white p-4 rounded-lg border border-slate-200 shadow-sm flex items-center gap-4">
            <div className="bg-red-100 p-3 rounded-full text-red-600">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs text-slate-500 font-medium uppercase">Total Defeitos</p>
              <p className="text-2xl font-bold text-slate-900">{metrics.totalDefects}</p>
            </div>
          </div>
        </div>
      )}

      {error && (
        <div className="rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700 flex items-center gap-2">
          <AlertTriangle className="h-4 w-4" />
          <span>{error}</span>
        </div>
      )}

      {/* Results Section */}
      {history.length > 0 && (
        <section className="rounded-lg border border-slate-200 bg-white shadow-sm overflow-hidden">
          <div className="border-b border-slate-200 px-4 py-3 flex justify-between items-center bg-slate-50/50">
            <h3 className="text-sm font-semibold text-slate-900">Histórico Detalhado</h3>
            <button
              onClick={handleExportCsv}
              className="inline-flex items-center gap-1.5 rounded-md border border-slate-300 bg-white px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50 transition"
            >
              <Download className="w-3.5 h-3.5" />
              Exportar CSV
            </button>
          </div>
          
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="bg-slate-50 text-slate-500 uppercase text-[10px] tracking-wider font-bold border-b border-slate-200">
                  <th className="px-4 py-3">Horário</th>
                  <th className="px-4 py-3">Máquina</th>
                  <th className="px-4 py-3">Turno</th>
                  <th className="px-4 py-3 text-center">Amostragem</th>
                  <th className="px-4 py-3 text-center">Qualidade</th>
                  <th className="px-4 py-3">Principais Defeitos</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {history.map((reg) => (
                  <tr key={reg.id} className="hover:bg-slate-50 transition">
                    <td className="px-4 py-3 font-medium text-slate-900">{formatApiDateTime(reg.hora)}</td>
                    <td className="px-4 py-3 text-slate-600">{reg.machineName}</td>
                    <td className="px-4 py-3 text-slate-600">{reg.turnoName}</td>
                    <td className="px-4 py-3 text-center font-bold text-blue-600">{reg.value}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-col items-center gap-1">
                        <span className={`text-xs font-bold ${reg.qualidadeParcial >= 95 ? 'text-emerald-600' : 'text-amber-600'}`}>
                          {reg.qualidadeParcial.toFixed(1)}%
                        </span>
                        <div className="w-16 bg-slate-200 h-1 rounded-full overflow-hidden">
                          <div 
                            className={`h-full ${reg.qualidadeParcial >= 95 ? 'bg-emerald-500' : 'bg-amber-500'}`} 
                            style={{ width: `${Math.min(100, Math.max(0, reg.qualidadeParcial))}%` }} 
                          />
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-1.5">
                        {reg.defeitos.length > 0 ? (
                          reg.defeitos.map(def => (
                            <span key={def.defeitoId} className="inline-flex flex-col border border-red-100 bg-red-50 text-red-700 px-2 py-0.5 rounded text-[11px]">
                              <span className="font-bold">{def.defeitoName}</span>
                              <div className="flex justify-between gap-2 opacity-80">
                                <span>Qtd: {def.value}</span>
                                <span>{( (def.value / (reg.value || 1)) * 100 ).toFixed(1)}%</span>
                              </div>
                            </span>
                          ))
                        ) : (
                          <span className="text-emerald-600 text-[11px] font-medium italic">Nenhum defeito</span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {!loading && history.length === 0 && !error && (
        <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50/50 p-12 text-center">
          <ShieldCheck className="w-10 h-10 text-slate-300 mx-auto mb-3" />
          <p className="text-slate-500 font-medium">
            {hasSearched
              ? 'Nenhum registro encontrado para os filtros selecionados.'
              : 'Use os filtros acima para pesquisar o histórico de qualidade.'}
          </p>
        </div>
      )}
    </div>
  );
};

export default QualitySearchPage;
