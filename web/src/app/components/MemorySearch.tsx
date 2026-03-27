import React, { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, Database, Download, Search } from 'lucide-react';
import { requestApi } from '../../services/api';

interface Device {
  id: number;
  name: string;
  mnemonic: string;
}

interface Fabrica {
  id: number;
  name: string;
}

interface MiniFabrica {
  id: number;
  name: string;
  fabricaId: number;
  setorIds: number[];
}

interface Setor {
  id: number;
  name: string;
}

interface Machine {
  id: number;
  name: string;
  deviceId: number;
  miniFabricaId: number;
  setorId: number;
}

interface Turno {
  id: number;
  name: string;
  horaInicio: string;
  horaFinal: string;
}

interface MemoryValueByDeviceDTO {
  deviceId: number;
  plcMnemonic: string;
  tagName: string;
  memoryName: string;
  value: number;
  timestamp: string;
}

export const MemorySearch: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'device' | 'structure'>('device');
  const [devices, setDevices] = useState<Device[]>([]);
  const [turnos, setTurnos] = useState<Turno[]>([]);
  const [fabricas, setFabricas] = useState<Fabrica[]>([]);
  const [miniFabricas, setMiniFabricas] = useState<MiniFabrica[]>([]);
  const [setores, setSetores] = useState<Setor[]>([]);
  const [machines, setMachines] = useState<Machine[]>([]);

  const [selectedFabricaId, setSelectedFabricaId] = useState('');
  const [selectedMiniFabricaId, setSelectedMiniFabricaId] = useState('');
  const [selectedSetorId, setSelectedSetorId] = useState('');
  const [selectedMachineId, setSelectedMachineId] = useState('');

  const [selectedMnemonic, setSelectedMnemonic] = useState<string>('');
  const [selectedTag, setSelectedTag] = useState('');
  const [selectedDate, setSelectedDate] = useState('');
  const [selectedTurnoId, setSelectedTurnoId] = useState('');
  const [memoryValues, setMemoryValues] = useState<MemoryValueByDeviceDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let alive = true;
    const loadBaseData = async () => {
      try {
        const [deviceData, turnoData, fabricaData, miniFabricaData, setorData, machineData] = await Promise.all([
          requestApi<Device[]>('/api/devices'),
          requestApi<Turno[]>('/api/turnos'),
          requestApi<Fabrica[]>('/api/fabricas'),
          requestApi<MiniFabrica[]>('/api/mini-fabricas'),
          requestApi<Setor[]>('/api/setores'),
          requestApi<Machine[]>('/api/machines'),
        ]);

        if (!alive) {
          return;
        }

        setDevices(deviceData);
        setTurnos(turnoData);
        setFabricas(fabricaData);
        setMiniFabricas(miniFabricaData);
        setSetores(setorData);
        setMachines(machineData);
      } catch (err) {
        if (alive) {
          setError(`Erro ao carregar filtros: ${err instanceof Error ? err.message : 'erro desconhecido'}`);
        }
      }
    };

    loadBaseData();
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    if (!selectedMnemonic) {
      setMemoryValues([]);
      setSelectedTag('');
      setSelectedDate('');
      setSelectedTurnoId('');
      setError(null);
      return;
    }

    const controller = new AbortController();
    setMemoryValues([]);
    setSelectedTag('');
    setSelectedDate('');
    setSelectedTurnoId('');
    setLoading(true);
    setError(null);

    const requestUrl = `/api/devices/${selectedMnemonic}/memory-values?_ts=${Date.now()}`;

    fetch(requestUrl, {
      signal: controller.signal,
      cache: 'no-store',
    })
      .then((res) => {
        if (res.status === 204) {
          return [];
        }
        if (!res.ok) {
          throw new Error('Nenhum dado encontrado.');
        }
        return res.json();
      })
      .then((data: MemoryValueByDeviceDTO[]) => {
        setMemoryValues(data);
      })
      .catch((err) => {
        if (err.name !== 'AbortError') {
          setError(err.message);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [selectedMnemonic]);

  const selectedDevice = useMemo(
    () => devices.find((device) => device.mnemonic === selectedMnemonic) ?? null,
    [devices, selectedMnemonic],
  );

  const filteredMiniFabricas = useMemo(() => {
    const fabricaId = Number(selectedFabricaId);
    if (!fabricaId) {
      return [];
    }
    return miniFabricas.filter((row) => row.fabricaId === fabricaId);
  }, [miniFabricas, selectedFabricaId]);

  const filteredSetores = useMemo(() => {
    const miniFabricaId = Number(selectedMiniFabricaId);
    if (!miniFabricaId) {
      return [];
    }
    const selectedMini = miniFabricas.find((row) => row.id === miniFabricaId);
    if (!selectedMini) {
      return [];
    }
    return setores.filter((row) => selectedMini.setorIds.includes(row.id));
  }, [setores, miniFabricas, selectedMiniFabricaId]);

  const filteredMachines = useMemo(() => {
    const setorId = Number(selectedSetorId);
    const miniFabricaId = Number(selectedMiniFabricaId);
    if (!setorId || !miniFabricaId) {
      return [];
    }
    return machines.filter((row) => row.setorId === setorId && row.miniFabricaId === miniFabricaId);
  }, [machines, selectedSetorId, selectedMiniFabricaId]);



  const selectedTurno = useMemo(() => {
    const id = Number(selectedTurnoId);
    if (!id) {
      return null;
    }
    return turnos.find((turno) => turno.id === id) ?? null;
  }, [turnos, selectedTurnoId]);

  const availableTags = useMemo(() => {
    return Array.from(new Set(memoryValues.map((mv) => mv.tagName))).sort((a, b) => a.localeCompare(b));
  }, [memoryValues]);

  const toLocalDateKey = (value: string) => {
    const dt = new Date(value);
    if (Number.isNaN(dt.getTime())) {
      return '';
    }
    const y = dt.getFullYear();
    const m = String(dt.getMonth() + 1).padStart(2, '0');
    const d = String(dt.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  };

  const toMinutesFromTime = (time: string) => {
    const [hh = '0', mm = '0'] = time.split(':');
    const h = Number(hh);
    const m = Number(mm);
    if (Number.isNaN(h) || Number.isNaN(m)) {
      return null;
    }
    return h * 60 + m;
  };

  const isInsideTurno = (timestamp: string, turno: Turno) => {
    const dt = new Date(timestamp);
    if (Number.isNaN(dt.getTime())) {
      return false;
    }

    const valueMin = dt.getHours() * 60 + dt.getMinutes();
    const startMin = toMinutesFromTime(turno.horaInicio);
    const endMin = toMinutesFromTime(turno.horaFinal);

    if (startMin === null || endMin === null) {
      return false;
    }

    if (startMin < endMin) {
      return valueMin > startMin && valueMin <= endMin;
    }

    if (startMin > endMin) {
      return valueMin > startMin || valueMin <= endMin;
    }

    return false;
  };

  const filteredMemoryValues = useMemo(() => {
    let values = memoryValues;

    if (selectedTag) {
      values = values.filter((mv) => mv.tagName.trim().toLowerCase() === selectedTag.trim().toLowerCase());
    }

    if (selectedDate) {
      values = values.filter((mv) => toLocalDateKey(mv.timestamp) === selectedDate);
    }

    if (selectedTurno) {
      values = values.filter((mv) => isInsideTurno(mv.timestamp, selectedTurno));
    }

    return values;
  }, [memoryValues, selectedTag, selectedDate, selectedTurno]);

  const formatTimestamp = (value: string) => {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString('pt-BR');
  };

  const toCsvField = (value: string | number) => {
    const text = String(value ?? '');
    const escaped = text.replaceAll('"', '""');
    return `"${escaped}"`;
  };

  const handleExportExcel = () => {
    if (filteredMemoryValues.length === 0) {
      return;
    }

    const headers = ['Device', 'Mnemonic', 'Tag', 'Memoria', 'Valor', 'Atualizado em'];
    const rows = filteredMemoryValues.map((mv) => [
      selectedDevice?.name ?? '',
      mv.plcMnemonic,
      mv.tagName,
      mv.memoryName,
      mv.value,
      formatTimestamp(mv.timestamp),
    ]);

    const csv = [headers.map(toCsvField).join(';'), ...rows.map((row) => row.map(toCsvField).join(';'))].join('\n');
    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' });
    const filename = `memory-search-${selectedMnemonic || 'filtros'}-${new Date().toISOString().slice(0, 10)}.csv`;

    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  };

  const onFabricaChange = (value: string) => {
    setSelectedFabricaId(value);
    setSelectedMiniFabricaId('');
    setSelectedSetorId('');
    setSelectedMachineId('');
    setSelectedMnemonic('');
  };

  const onMiniFabricaChange = (value: string) => {
    setSelectedMiniFabricaId(value);
    setSelectedSetorId('');
    setSelectedMachineId('');
    setSelectedMnemonic('');
  };

  const onSetorChange = (value: string) => {
    setSelectedSetorId(value);
    setSelectedMachineId('');
    setSelectedMnemonic('');
  };

  const onMachineChange = (value: string) => {
    setSelectedMachineId(value);
    if (!value) {
      setSelectedMnemonic('');
      return;
    }
    const selectedMachine = machines.find((row) => row.id === Number(value));
    const selectedById = devices.find((row) => row.id === selectedMachine?.deviceId);
    setSelectedMnemonic(selectedById?.mnemonic ?? '');
  };

  return (
    <div className="space-y-4">
      <section className="rounded-lg border border-slate-200 bg-white px-4 py-3 shadow-sm">
        <div className="flex items-center gap-2">
          <Search className="h-4 w-4 text-blue-600" />
          <h2 className="text-lg font-semibold text-slate-900">Pesquisa de Memorias</h2>
        </div>
        <p className="mt-1 text-sm text-slate-600">Consulte os ultimos valores de memoria por device.</p>
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <div className="mb-4 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setActiveTab('device')}
            className={`rounded-md px-3 py-2 text-sm font-medium transition ${
              activeTab === 'device'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-100'
            }`}
          >
            Pesquisa por device
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('structure')}
            className={`rounded-md px-3 py-2 text-sm font-medium transition ${
              activeTab === 'structure'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-100'
            }`}
          >
            Pesquisa por estrutura
          </button>
        </div>

        {activeTab === 'device' && (
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div>
              <label htmlFor="device-select" className="mb-1 block text-sm font-medium text-slate-700">
                Selecione um device
              </label>
              <select
                id="device-select"
                value={selectedMnemonic}
                onChange={(e) => setSelectedMnemonic(e.target.value)}
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
              >
                <option value="">Selecione...</option>
                {devices.map((device) => (
                  <option key={device.id} value={device.mnemonic}>
                    {device.name} ({device.mnemonic})
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="turno-select-device" className="mb-1 block text-sm font-medium text-slate-700">
                Selecione o turno
              </label>
              <select
                id="turno-select-device"
                value={selectedTurnoId}
                onChange={(e) => setSelectedTurnoId(e.target.value)}
                className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
              >
                <option value="">Todos os turnos</option>
                {turnos.map((turno) => (
                  <option key={turno.id} value={String(turno.id)}>
                    {turno.name} ({turno.horaInicio} - {turno.horaFinal})
                  </option>
                ))}
              </select>
            </div>
          </div>
        )}

        {activeTab === 'structure' && (
          <div className="space-y-3">
            <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
              <div>
                <label htmlFor="fabrica-select" className="mb-1 block text-sm font-medium text-slate-700">
                  Fabrica
                </label>
                <select
                  id="fabrica-select"
                  value={selectedFabricaId}
                  onChange={(e) => onFabricaChange(e.target.value)}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                >
                  <option value="">Selecione...</option>
                  {fabricas.map((fabrica) => (
                    <option key={fabrica.id} value={String(fabrica.id)}>
                      {fabrica.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="mini-fabrica-select" className="mb-1 block text-sm font-medium text-slate-700">
                  Mini Fabrica
                </label>
                <select
                  id="mini-fabrica-select"
                  value={selectedMiniFabricaId}
                  onChange={(e) => onMiniFabricaChange(e.target.value)}
                  disabled={!selectedFabricaId}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition disabled:cursor-not-allowed disabled:bg-slate-100 focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                >
                  <option value="">Selecione...</option>
                  {filteredMiniFabricas.map((miniFabrica) => (
                    <option key={miniFabrica.id} value={String(miniFabrica.id)}>
                      {miniFabrica.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="setor-select" className="mb-1 block text-sm font-medium text-slate-700">
                  Setor
                </label>
                <select
                  id="setor-select"
                  value={selectedSetorId}
                  onChange={(e) => onSetorChange(e.target.value)}
                  disabled={!selectedMiniFabricaId}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition disabled:cursor-not-allowed disabled:bg-slate-100 focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                >
                  <option value="">Selecione...</option>
                  {filteredSetores.map((setor) => (
                    <option key={setor.id} value={String(setor.id)}>
                      {setor.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="machine-select" className="mb-1 block text-sm font-medium text-slate-700">
                  Machine
                </label>
                <select
                  id="machine-select"
                  value={selectedMachineId}
                  onChange={(e) => onMachineChange(e.target.value)}
                  disabled={!selectedSetorId}
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 outline-none transition disabled:cursor-not-allowed disabled:bg-slate-100 focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                >
                  <option value="">Selecione...</option>
                  {filteredMachines.map((machine) => (
                    <option key={machine.id} value={String(machine.id)}>
                      {machine.name}
                    </option>
                  ))}
                </select>
              </div>

            </div>

            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
              <div>
                <label htmlFor="turno-select-structure" className="mb-1 block text-sm font-medium text-slate-700">
                  Selecione o turno
                </label>
                <select
                  id="turno-select-structure"
                  value={selectedTurnoId}
                  onChange={(e) => setSelectedTurnoId(e.target.value)}
                  className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                >
                  <option value="">Todos os turnos</option>
                  {turnos.map((turno) => (
                    <option key={turno.id} value={String(turno.id)}>
                      {turno.name} ({turno.horaInicio} - {turno.horaFinal})
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>
        )}
      </section>

      {error && (
        <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
          <AlertTriangle className="h-4 w-4" />
          <span>{error}</span>
        </div>
      )}

      {loading && (
        <div className="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-600 shadow-sm">
          Carregando dados...
        </div>
      )}

      {!loading && selectedMnemonic && memoryValues.length === 0 && !error && (
        <div className="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-600 shadow-sm">
          Nenhuma memoria encontrada para o device selecionado.
        </div>
      )}

      {!loading && memoryValues.length > 0 && (
        <section className="rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="flex flex-col gap-2 border-b border-slate-200 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-2">
              <Database className="h-4 w-4 text-blue-600" />
              <h3 className="text-sm font-semibold text-slate-900">Resultados de memoria</h3>
            </div>
            <div className="flex items-center gap-2">
              <div className="text-xs text-slate-600">
                {selectedDevice
                  ? `${selectedDevice.name} (${selectedDevice.mnemonic})`
                  : selectedMnemonic}{' '}
                | {filteredMemoryValues.length} de {memoryValues.length} registros
              </div>
              <button
                type="button"
                onClick={handleExportExcel}
                disabled={filteredMemoryValues.length === 0}
                className="inline-flex items-center gap-1 rounded-md border border-slate-300 bg-white px-2 py-1 text-xs font-medium text-slate-700 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <Download className="h-3.5 w-3.5" />
                Exportar Excel
              </button>
            </div>
          </div>

          <div className="max-h-[60vh] overflow-auto">
            <table key={`${selectedMnemonic}-${selectedTag || 'none'}-${selectedDate || 'all'}-${selectedTurnoId || 'all'}-${memoryValues.length}`} className="min-w-full text-sm">
              <thead className="sticky top-0 z-10 bg-slate-50 text-slate-600 shadow-sm">
                <tr>
                  <th className="px-4 py-2 text-left font-medium">
                    <div className="flex min-w-[240px] items-center gap-2">
                      <span>Tag</span>
                      <select
                        value={selectedTag}
                        onChange={(e) => setSelectedTag(e.target.value)}
                        className="min-w-[160px] rounded-md border border-slate-300 bg-white px-2 py-1 text-xs font-normal text-slate-800 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                      >
                        <option value="">Todas as TAGs</option>
                        {availableTags.map((tag) => (
                          <option key={tag} value={tag}>
                            {tag}
                          </option>
                        ))}
                      </select>
                    </div>
                  </th>
                  <th className="px-4 py-2 text-left font-medium">Memoria</th>
                  <th className="px-4 py-2 text-left font-medium">Valor</th>
                  <th className="px-4 py-2 text-left font-medium">
                    <div className="flex min-w-[220px] items-center gap-2">
                      <span>Atualizado em</span>
                      <input
                        type="date"
                        value={selectedDate}
                        onChange={(e) => setSelectedDate(e.target.value)}
                        onKeyDown={(e) => e.preventDefault()}
                        onPaste={(e) => e.preventDefault()}
                        onDrop={(e) => e.preventDefault()}
                        inputMode="none"
                        className="rounded-md border border-slate-300 bg-white px-2 py-1 text-xs font-normal text-slate-800 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                      />
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody>
                {filteredMemoryValues.map((mv, index) => (
                  <tr
                    key={`${mv.deviceId}-${mv.tagName}-${mv.memoryName}-${mv.timestamp}-${mv.value}`}
                    className={`border-t border-slate-200 ${index % 2 === 0 ? 'bg-white' : 'bg-slate-200'} hover:bg-blue-100`}
                  >
                    <td className="px-4 py-2 text-slate-800">{mv.tagName}</td>
                    <td className="px-4 py-2 text-slate-800">{mv.memoryName}</td>
                    <td className="px-4 py-2 font-medium text-slate-900">{mv.value}</td>
                    <td className="px-4 py-2 text-slate-700">{formatTimestamp(mv.timestamp)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {!loading && selectedMnemonic && memoryValues.length > 0 && filteredMemoryValues.length === 0 && !error && (
        <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800 shadow-sm">
          Nenhuma memoria encontrada com os filtros selecionados.
        </div>
      )}
    </div>
  );
};

export default MemorySearch;


