import { useEffect, useMemo, useState } from 'react';
import { Building2, Cog, Clock3, Layers3, Map as MapIcon, AlertCircle } from 'lucide-react';
import { Fabrica, MiniFabrica, Setor, Machine, Device, Turno, Defeito, ViewMessage } from './types';
import { requestApi } from '../../../services/api';

import { FabricaSection } from './FabricaSection';
import { MiniFabricaSection } from './MiniFabricaSection';
import { SetorSection } from './SetorSection';
import { TurnoSection } from './TurnoSection';
import { MachineSection } from './MachineSection';
import { DefeitoSection } from './DefeitoSection';

type SectionKey = 'fabrica' | 'mini_fabrica' | 'setor' | 'turno' | 'machine' | 'defeito';

export function AdminCrudPage() {
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<ViewMessage | null>(null);
  const [activeSection, setActiveSection] = useState<SectionKey>('fabrica');

  const [fabricas, setFabricas] = useState<Fabrica[]>([]);
  const [miniFabricas, setMiniFabricas] = useState<MiniFabrica[]>([]);
  const [setores, setSetores] = useState<Setor[]>([]);
  const [machines, setMachines] = useState<Machine[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [turnos, setTurnos] = useState<Turno[]>([]);
  const [defeitos, setDefeitos] = useState<Defeito[]>([]);

  const sections: { key: SectionKey; label: string; icon: JSX.Element }[] = [
    { key: 'fabrica', label: 'Fabricas', icon: <Building2 className="h-4 w-4 text-blue-600" /> },
    { key: 'mini_fabrica', label: 'Mini Fabricas', icon: <Layers3 className="h-4 w-4 text-indigo-600" /> },
    { key: 'setor', label: 'Setores', icon: <MapIcon className="h-4 w-4 text-amber-600" /> },
    { key: 'turno', label: 'Turnos', icon: <Clock3 className="h-4 w-4 text-violet-600" /> },
    { key: 'machine', label: 'Machines', icon: <Cog className="h-4 w-4 text-emerald-600" /> },
    { key: 'defeito', label: 'Defeitos', icon: <AlertCircle className="h-4 w-4 text-red-600" /> },
  ];

  const fabricaById = useMemo(() => {
    const map = new Map<number, string>();
    fabricas.forEach((row) => map.set(row.id, row.name));
    return map;
  }, [fabricas]);

  const miniFabricaById = useMemo(() => {
    const map = new Map<number, string>();
    miniFabricas.forEach((row) => map.set(row.id, row.name));
    return map;
  }, [miniFabricas]);

  const setorById = useMemo(() => {
    const map = new Map<number, string>();
    setores.forEach((row) => map.set(row.id, row.name));
    return map;
  }, [setores]);

  const deviceById = useMemo(() => {
    const map = new Map<number, string>();
    devices.forEach((row) => map.set(row.id, `${row.mnemonic} - ${row.name}`));
    return map;
  }, [devices]);

  const loadAll = async () => {
    const [fabricaData, miniData, setorData, machineData, deviceData, turnoData, defeitoData] = await Promise.all([
      requestApi<Fabrica[]>('/api/fabricas'),
      requestApi<MiniFabrica[]>('/api/mini-fabricas'),
      requestApi<Setor[]>('/api/setores'),
      requestApi<Machine[]>('/api/machines'),
      requestApi<Device[]>('/api/devices'),
      requestApi<Turno[]>('/api/turnos'),
      requestApi<Defeito[]>('/api/defeitos'),
    ]);
    setFabricas(fabricaData);
    setMiniFabricas(miniData);
    setSetores(setorData);
    setMachines(machineData);
    setDevices(deviceData);
    setTurnos(turnoData);
    setDefeitos(defeitoData);
  };

  useEffect(() => {
    let alive = true;
    const run = async () => {
      try {
        await loadAll();
      } catch (err) {
        if (alive) {
          setMessage({
            type: 'error',
            text: err instanceof Error ? err.message : 'Falha ao carregar dados',
          });
        }
      } finally {
        if (alive) {
          setIsLoading(false);
        }
      }
    };
    run();
    return () => {
      alive = false;
    };
  }, []);

  const withMutation = async (work: () => Promise<void>, successText: string) => {
    try {
      setIsSaving(true);
      setMessage(null);
      await work();
      await loadAll();
      setMessage({ type: 'success', text: successText });
    } catch (err) {
      setMessage({
        type: 'error',
        text: err instanceof Error ? err.message : 'Operacao falhou',
      });
    } finally {
      setIsSaving(false);
    }
  };

  const removeItem = async (url: string, successText: string) => {
    await withMutation(async () => {
      await requestApi<void>(url, { method: 'DELETE' });
    }, successText);
  };

  const renderSection = () => {
    if (activeSection === 'fabrica') {
      return (
        <FabricaSection
          fabricas={fabricas}
          isSaving={isSaving}
          withMutation={withMutation}
          removeItem={removeItem}
        />
      );
    }

    if (activeSection === 'mini_fabrica') {
      return (
        <MiniFabricaSection
          miniFabricas={miniFabricas}
          fabricas={fabricas}
          setores={setores}
          fabricaById={fabricaById}
          setorById={setorById}
          isSaving={isSaving}
          withMutation={withMutation}
          removeItem={removeItem}
        />
      );
    }

    if (activeSection === 'setor') {
      return (
        <SetorSection
          setores={setores}
          isSaving={isSaving}
          withMutation={withMutation}
          removeItem={removeItem}
        />
      );
    }

    if (activeSection === 'turno') {
      return (
        <TurnoSection
          turnos={turnos}
          isSaving={isSaving}
          withMutation={withMutation}
          removeItem={removeItem}
        />
      );
    }

    if (activeSection === 'defeito') {
      return (
        <DefeitoSection
          defeitos={defeitos}
          isSaving={isSaving}
          withMutation={withMutation}
          removeItem={removeItem}
        />
      );
    }

    return (
      <MachineSection
        machines={machines}
        devices={devices}
        miniFabricas={miniFabricas}
        setores={setores}
        miniFabricaById={miniFabricaById}
        deviceById={deviceById}
        setorById={setorById}
        isSaving={isSaving}
        withMutation={withMutation}
        removeItem={removeItem}
      />
    );
  };

  return (
    <div className="space-y-4">
      <div className="rounded-lg border border-slate-200 bg-white px-4 py-3 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Cadastro de Estrutura</h2>
        <p className="text-sm text-slate-600">
          Gerencie Fabrica, Mini Fabrica, Setor, Turno, Machine e Defeito para estruturar a linha de producao.
        </p>
      </div>

      {message && (
        <div
          className={`rounded-lg border px-3 py-2 text-sm ${
            message.type === 'success'
              ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
              : 'border-red-200 bg-red-50 text-red-700'
          }`}
        >
          {message.text}
        </div>
      )}

      {isLoading ? (
        <div className="rounded-lg border border-slate-200 bg-white p-6 text-sm text-slate-600 shadow-sm">
          Carregando dados...
        </div>
      ) : (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center gap-2 rounded-lg border border-slate-200 bg-white/80 p-3 shadow-sm">
            {sections.map((section) => (
              <button
                key={section.key}
                type="button"
                onClick={() => setActiveSection(section.key)}
                className={`flex items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium transition ${
                  activeSection === section.key
                    ? 'bg-slate-900 text-white'
                    : 'text-slate-700 hover:bg-slate-100'
                }`}
              >
                {section.icon}
                {section.label}
              </button>
            ))}
          </div>

          {renderSection()}
        </div>
      )}
    </div>
  );
}
