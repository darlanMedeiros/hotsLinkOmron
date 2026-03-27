import { useEffect, useMemo, useState } from 'react';
import { Fabrica, MiniFabrica, Setor, Machine, Device, Turno, ViewMessage } from './types';
import { requestApi } from '../../../services/api';

import { FabricaSection } from './FabricaSection';
import { MiniFabricaSection } from './MiniFabricaSection';
import { SetorSection } from './SetorSection';
import { TurnoSection } from './TurnoSection';
import { MachineSection } from './MachineSection';

export function AdminCrudPage() {
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<ViewMessage | null>(null);

  const [fabricas, setFabricas] = useState<Fabrica[]>([]);
  const [miniFabricas, setMiniFabricas] = useState<MiniFabrica[]>([]);
  const [setores, setSetores] = useState<Setor[]>([]);
  const [machines, setMachines] = useState<Machine[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [turnos, setTurnos] = useState<Turno[]>([]);

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
    const [fabricaData, miniData, setorData, machineData, deviceData, turnoData] = await Promise.all([
      requestApi<Fabrica[]>('/api/fabricas'),
      requestApi<MiniFabrica[]>('/api/mini-fabricas'),
      requestApi<Setor[]>('/api/setores'),
      requestApi<Machine[]>('/api/machines'),
      requestApi<Device[]>('/api/devices'),
      requestApi<Turno[]>('/api/turnos'),
    ]);
    setFabricas(fabricaData);
    setMiniFabricas(miniData);
    setSetores(setorData);
    setMachines(machineData);
    setDevices(deviceData);
    setTurnos(turnoData);
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

  return (
    <div className="space-y-4">
      <div className="rounded-lg border border-slate-200 bg-white px-4 py-3 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Cadastro de estrutura</h2>
        <p className="text-sm text-slate-600">
          Gerencie Fabrica, Mini Fabrica, Setor e Machine com operacoes de criar, editar, excluir e consultar.
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
        <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
          <FabricaSection
            fabricas={fabricas}
            isSaving={isSaving}
            withMutation={withMutation}
            removeItem={removeItem}
          />
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
          <SetorSection
            setores={setores}
            isSaving={isSaving}
            withMutation={withMutation}
            removeItem={removeItem}
          />
          <TurnoSection
            turnos={turnos}
            isSaving={isSaving}
            withMutation={withMutation}
            removeItem={removeItem}
          />
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
        </div>
      )}
    </div>
  );
}
