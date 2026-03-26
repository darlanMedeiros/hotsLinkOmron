import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Building2, Clock3, Cog, Layers3, Map as MapIcon, Pencil, Save, Trash2, X } from 'lucide-react';
import { requestApi } from '../../services/api';

type ViewMessage = {
  type: 'success' | 'error';
  text: string;
};

type Fabrica = {
  id: number;
  name: string;
};

type MiniFabrica = {
  id: number;
  name: string;
  fabricaId: number;
};

type Setor = {
  id: number;
  name: string;
  miniFabricaId: number;
};

type Machine = {
  id: number;
  name: string;
  deviceId: number;
  setorId: number;
};

type Device = {
  id: number;
  mnemonic: string;
  name: string;
};

type Turno = {
  id: number;
  name: string;
  horaInicio: string;
  horaFinal: string;
};



export function AdminCrudScreen() {
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<ViewMessage | null>(null);

  const [fabricas, setFabricas] = useState<Fabrica[]>([]);
  const [miniFabricas, setMiniFabricas] = useState<MiniFabrica[]>([]);
  const [setores, setSetores] = useState<Setor[]>([]);
  const [machines, setMachines] = useState<Machine[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [turnos, setTurnos] = useState<Turno[]>([]);

  const [newFabricaName, setNewFabricaName] = useState('');
  const [newMiniFabricaName, setNewMiniFabricaName] = useState('');
  const [newMiniFabricaFabricaId, setNewMiniFabricaFabricaId] = useState<number | ''>('');
  const [newSetorName, setNewSetorName] = useState('');
  const [newSetorMiniFabricaId, setNewSetorMiniFabricaId] = useState<number | ''>('');
  const [newMachineName, setNewMachineName] = useState('');
  const [newMachineDeviceId, setNewMachineDeviceId] = useState<number | ''>('');
  const [newMachineSetorId, setNewMachineSetorId] = useState<number | ''>('');
  const [newTurnoName, setNewTurnoName] = useState('');
  const [newTurnoHoraInicio, setNewTurnoHoraInicio] = useState('');
  const [newTurnoHoraFinal, setNewTurnoHoraFinal] = useState('');

  const [editingFabrica, setEditingFabrica] = useState<Fabrica | null>(null);
  const [editingMiniFabrica, setEditingMiniFabrica] = useState<MiniFabrica | null>(null);
  const [editingSetor, setEditingSetor] = useState<Setor | null>(null);
  const [editingMachine, setEditingMachine] = useState<Machine | null>(null);
  const [editingTurno, setEditingTurno] = useState<Turno | null>(null);

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

  const onCreateFabrica = async (e: FormEvent) => {
    e.preventDefault();
    await withMutation(async () => {
      await requestApi<Fabrica>('/api/fabricas', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: newFabricaName }),
      });
      setNewFabricaName('');
    }, 'Fabrica criada');
  };

  const onCreateMiniFabrica = async (e: FormEvent) => {
    e.preventDefault();
    await withMutation(async () => {
      await requestApi<MiniFabrica>('/api/mini-fabricas', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: newMiniFabricaName, fabricaId: Number(newMiniFabricaFabricaId) }),
      });
      setNewMiniFabricaName('');
      setNewMiniFabricaFabricaId('');
    }, 'Mini fabrica criada');
  };

  const onCreateSetor = async (e: FormEvent) => {
    e.preventDefault();
    await withMutation(async () => {
      await requestApi<Setor>('/api/setores', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: newSetorName, miniFabricaId: Number(newSetorMiniFabricaId) }),
      });
      setNewSetorName('');
      setNewSetorMiniFabricaId('');
    }, 'Setor criado');
  };

  const onCreateMachine = async (e: FormEvent) => {
    e.preventDefault();
    await withMutation(async () => {
      await requestApi<Machine>('/api/machines', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: newMachineName,
          deviceId: Number(newMachineDeviceId),
          setorId: Number(newMachineSetorId),
        }),
      });
      setNewMachineName('');
      setNewMachineDeviceId('');
      setNewMachineSetorId('');
    }, 'Machine criada');
  };


  const onCreateTurno = async (e: FormEvent) => {
    e.preventDefault();
    await withMutation(async () => {
      await requestApi<Turno>('/api/turnos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: newTurnoName,
          horaInicio: newTurnoHoraInicio,
          horaFinal: newTurnoHoraFinal,
        }),
      });
      setNewTurnoName('');
      setNewTurnoHoraInicio('');
      setNewTurnoHoraFinal('');
    }, 'Turno criado');
  };

  const removeItem = async (url: string, successText: string) => {
    await withMutation(async () => {
      await requestApi<void>(url, { method: 'DELETE' });
    }, successText);
  };

  const onSaveFabrica = async () => {
    if (!editingFabrica) {
      return;
    }
    await withMutation(async () => {
      await requestApi<Fabrica>(`/api/fabricas/${editingFabrica.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: editingFabrica.name }),
      });
      setEditingFabrica(null);
    }, 'Fabrica atualizada');
  };

  const onSaveMiniFabrica = async () => {
    if (!editingMiniFabrica) {
      return;
    }
    await withMutation(async () => {
      await requestApi<MiniFabrica>(`/api/mini-fabricas/${editingMiniFabrica.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: editingMiniFabrica.name,
          fabricaId: editingMiniFabrica.fabricaId,
        }),
      });
      setEditingMiniFabrica(null);
    }, 'Mini fabrica atualizada');
  };

  const onSaveSetor = async () => {
    if (!editingSetor) {
      return;
    }
    await withMutation(async () => {
      await requestApi<Setor>(`/api/setores/${editingSetor.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: editingSetor.name,
          miniFabricaId: editingSetor.miniFabricaId,
        }),
      });
      setEditingSetor(null);
    }, 'Setor atualizado');
  };

  const onSaveMachine = async () => {
    if (!editingMachine) {
      return;
    }
    await withMutation(async () => {
      await requestApi<Machine>(`/api/machines/${editingMachine.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: editingMachine.name,
          deviceId: editingMachine.deviceId,
          setorId: editingMachine.setorId,
        }),
      });
      setEditingMachine(null);
    }, 'Machine atualizada');
  };


  const onSaveTurno = async () => {
    if (!editingTurno) {
      return;
    }
    await withMutation(async () => {
      await requestApi<Turno>(`/api/turnos/${editingTurno.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: editingTurno.name,
          horaInicio: editingTurno.horaInicio,
          horaFinal: editingTurno.horaFinal,
        }),
      });
      setEditingTurno(null);
    }, 'Turno atualizado');
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
          <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <div className="mb-3 flex items-center gap-2">
              <Building2 className="h-4 w-4 text-blue-600" />
              <h3 className="font-semibold text-slate-900">Fabrica</h3>
            </div>
            <form onSubmit={onCreateFabrica} className="mb-3 flex gap-2">
              <input
                value={newFabricaName}
                onChange={(e) => setNewFabricaName(e.target.value)}
                placeholder="Nome da fabrica"
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
              <button disabled={isSaving} className="rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white">
                Criar
              </button>
            </form>
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="text-left text-slate-600">
                    <th className="pb-2 pr-2">ID</th>
                    <th className="pb-2 pr-2">Nome</th>
                    <th className="pb-2 text-right">Acoes</th>
                  </tr>
                </thead>
                <tbody>
                  {fabricas.map((row, index) => {
                    const editing = editingFabrica?.id === row.id;
                    return (
                      <tr key={row.id} className={`border-t border-slate-200 ${index % 2 === 0 ? 'bg-white' : 'bg-slate-200'} hover:bg-blue-100`}>
                        <td className="py-2 pr-2 text-slate-700">{row.id}</td>
                        <td className="py-2 pr-2">
                          {editing ? (
                            <input
                              value={editingFabrica.name}
                              onChange={(e) => setEditingFabrica({ ...editingFabrica, name: e.target.value })}
                              className="w-full rounded-md border border-slate-300 px-2 py-1"
                            />
                          ) : (
                            <span>{row.name}</span>
                          )}
                        </td>
                        <td className="py-2 text-right">
                          <div className="inline-flex gap-1">
                            {editing ? (
                              <>
                                <button type="button" onClick={onSaveFabrica} className="rounded p-1 text-emerald-700 hover:bg-emerald-50">
                                  <Save className="h-4 w-4" />
                                </button>
                                <button type="button" onClick={() => setEditingFabrica(null)} className="rounded p-1 text-slate-600 hover:bg-slate-100">
                                  <X className="h-4 w-4" />
                                </button>
                              </>
                            ) : (
                              <>
                                <button type="button" onClick={() => setEditingFabrica(row)} className="rounded p-1 text-blue-700 hover:bg-blue-50">
                                  <Pencil className="h-4 w-4" />
                                </button>
                                <button type="button" onClick={() => removeItem(`/api/fabricas/${row.id}`, 'Fabrica removida')} className="rounded p-1 text-red-700 hover:bg-red-50">
                                  <Trash2 className="h-4 w-4" />
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>

          <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <div className="mb-3 flex items-center gap-2">
              <Layers3 className="h-4 w-4 text-indigo-600" />
              <h3 className="font-semibold text-slate-900">Mini Fabrica</h3>
            </div>
            <form onSubmit={onCreateMiniFabrica} className="mb-3 grid grid-cols-1 gap-2 sm:grid-cols-3">
              <input
                value={newMiniFabricaName}
                onChange={(e) => setNewMiniFabricaName(e.target.value)}
                placeholder="Nome"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm sm:col-span-2"
              />
              <select
                value={newMiniFabricaFabricaId}
                onChange={(e) => setNewMiniFabricaFabricaId(e.target.value ? Number(e.target.value) : '')}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              >
                <option value="">Selecione a fabrica</option>
                {fabricas.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.name} ({f.id})
                  </option>
                ))}
              </select>
              <button disabled={isSaving} className="rounded-md bg-indigo-600 px-3 py-2 text-sm font-medium text-white sm:col-span-3">
                Criar
              </button>
            </form>
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="text-left text-slate-600">
                    <th className="pb-2 pr-2">ID</th>
                    <th className="pb-2 pr-2">Nome</th>
                    <th className="pb-2 pr-2">Fabrica</th>
                    <th className="pb-2 text-right">Acoes</th>
                  </tr>
                </thead>
                <tbody>
                  {miniFabricas.map((row, index) => {
                    const editing = editingMiniFabrica?.id === row.id;
                    return (
                      <tr key={row.id} className={`border-t border-slate-200 ${index % 2 === 0 ? 'bg-white' : 'bg-slate-200'} hover:bg-blue-100`}>
                        <td className="py-2 pr-2">{row.id}</td>
                        <td className="py-2 pr-2">
                          {editing ? (
                            <input
                              value={editingMiniFabrica.name}
                              onChange={(e) => setEditingMiniFabrica({ ...editingMiniFabrica, name: e.target.value })}
                              className="w-full rounded-md border border-slate-300 px-2 py-1"
                            />
                          ) : (
                            row.name
                          )}
                        </td>
                        <td className="py-2 pr-2">
                          {editing ? (
                            <select
                              value={editingMiniFabrica.fabricaId}
                              onChange={(e) => setEditingMiniFabrica({ ...editingMiniFabrica, fabricaId: Number(e.target.value) })}
                              className="rounded-md border border-slate-300 px-2 py-1"
                            >
                              {fabricas.map((f) => (
                                <option key={f.id} value={f.id}>
                                  {f.name} ({f.id})
                                </option>
                              ))}
                            </select>
                          ) : (
                            `${fabricaById.get(row.fabricaId) ?? 'ID'} (${row.fabricaId})`
                          )}
                        </td>
                        <td className="py-2 text-right">
                          <div className="inline-flex gap-1">
                            {editing ? (
                              <>
                                <button type="button" onClick={onSaveMiniFabrica} className="rounded p-1 text-emerald-700 hover:bg-emerald-50">
                                  <Save className="h-4 w-4" />
                                </button>
                                <button type="button" onClick={() => setEditingMiniFabrica(null)} className="rounded p-1 text-slate-600 hover:bg-slate-100">
                                  <X className="h-4 w-4" />
                                </button>
                              </>
                            ) : (
                              <>
                                <button type="button" onClick={() => setEditingMiniFabrica(row)} className="rounded p-1 text-blue-700 hover:bg-blue-50">
                                  <Pencil className="h-4 w-4" />
                                </button>
                                <button type="button" onClick={() => removeItem(`/api/mini-fabricas/${row.id}`, 'Mini fabrica removida')} className="rounded p-1 text-red-700 hover:bg-red-50">
                                  <Trash2 className="h-4 w-4" />
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>

          <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <div className="mb-3 flex items-center gap-2">
              <MapIcon className="h-4 w-4 text-amber-600" />
              <h3 className="font-semibold text-slate-900">Setor</h3>
            </div>
            <form onSubmit={onCreateSetor} className="mb-3 grid grid-cols-1 gap-2 sm:grid-cols-3">
              <input
                value={newSetorName}
                onChange={(e) => setNewSetorName(e.target.value)}
                placeholder="Nome"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm sm:col-span-2"
              />
              <select
                value={newSetorMiniFabricaId}
                onChange={(e) => setNewSetorMiniFabricaId(e.target.value ? Number(e.target.value) : '')}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              >
                <option value="">Selecione a mini fabrica</option>
                {miniFabricas.map((mf) => (
                  <option key={mf.id} value={mf.id}>
                    {mf.name} ({mf.id})
                  </option>
                ))}
              </select>
              <button disabled={isSaving} className="rounded-md bg-amber-600 px-3 py-2 text-sm font-medium text-white sm:col-span-3">
                Criar
              </button>
            </form>
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="text-left text-slate-600">
                    <th className="pb-2 pr-2">ID</th>
                    <th className="pb-2 pr-2">Nome</th>
                    <th className="pb-2 pr-2">Mini Fabrica</th>
                    <th className="pb-2 text-right">Acoes</th>
                  </tr>
                </thead>
                <tbody>
                  {setores.map((row, index) => {
                    const editing = editingSetor?.id === row.id;
                    return (
                      <tr key={row.id} className={`border-t border-slate-200 ${index % 2 === 0 ? 'bg-white' : 'bg-slate-200'} hover:bg-blue-100`}>
                        <td className="py-2 pr-2">{row.id}</td>
                        <td className="py-2 pr-2">
                          {editing ? (
                            <input
                              value={editingSetor.name}
                              onChange={(e) => setEditingSetor({ ...editingSetor, name: e.target.value })}
                              className="w-full rounded-md border border-slate-300 px-2 py-1"
                            />
                          ) : (
                            row.name
                          )}
                        </td>
                        <td className="py-2 pr-2">
                          {editing ? (
                            <select
                              value={editingSetor.miniFabricaId}
                              onChange={(e) => setEditingSetor({ ...editingSetor, miniFabricaId: Number(e.target.value) })}
                              className="rounded-md border border-slate-300 px-2 py-1"
                            >
                              {miniFabricas.map((mf) => (
                                <option key={mf.id} value={mf.id}>
                                  {mf.name} ({mf.id})
                                </option>
                              ))}
                            </select>
                          ) : (
                            `${miniFabricaById.get(row.miniFabricaId) ?? 'ID'} (${row.miniFabricaId})`
                          )}
                        </td>
                        <td className="py-2 text-right">
                          <div className="inline-flex gap-1">
                            {editing ? (
                              <>
                                <button type="button" onClick={onSaveSetor} className="rounded p-1 text-emerald-700 hover:bg-emerald-50">
                                  <Save className="h-4 w-4" />
                                </button>
                                <button type="button" onClick={() => setEditingSetor(null)} className="rounded p-1 text-slate-600 hover:bg-slate-100">
                                  <X className="h-4 w-4" />
                                </button>
                              </>
                            ) : (
                              <>
                                <button type="button" onClick={() => setEditingSetor(row)} className="rounded p-1 text-blue-700 hover:bg-blue-50">
                                  <Pencil className="h-4 w-4" />
                                </button>
                                <button type="button" onClick={() => removeItem(`/api/setores/${row.id}`, 'Setor removido')} className="rounded p-1 text-red-700 hover:bg-red-50">
                                  <Trash2 className="h-4 w-4" />
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>

          <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <div className="mb-3 flex items-center gap-2">
              <Clock3 className="h-4 w-4 text-violet-600" />
              <h3 className="font-semibold text-slate-900">Turno</h3>
            </div>
            <form onSubmit={onCreateTurno} className="mb-3 grid grid-cols-1 gap-2 sm:grid-cols-3">
              <input
                value={newTurnoName}
                onChange={(e) => setNewTurnoName(e.target.value)}
                placeholder="Nome do turno"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
              <input
                type="time"
                value={newTurnoHoraInicio}
                onChange={(e) => setNewTurnoHoraInicio(e.target.value)}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
              <input
                type="time"
                value={newTurnoHoraFinal}
                onChange={(e) => setNewTurnoHoraFinal(e.target.value)}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
              <button disabled={isSaving} className="rounded-md bg-violet-600 px-3 py-2 text-sm font-medium text-white sm:col-span-3">
                Criar
              </button>
            </form>
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="text-left text-slate-600">
                    <th className="pb-2 pr-2">ID</th>
                    <th className="pb-2 pr-2">Nome</th>
                    <th className="pb-2 pr-2">Hora Inicio</th>
                    <th className="pb-2 pr-2">Hora Final</th>
                    <th className="pb-2 text-right">Acoes</th>
                  </tr>
                </thead>
                <tbody>
                  {turnos.map((row, index) => {
                    const editing = editingTurno?.id === row.id;
                    return (
                      <tr key={row.id} className={`border-t border-slate-200 ${index % 2 === 0 ? 'bg-white' : 'bg-slate-200'} hover:bg-blue-100`}>
                        <td className="py-2 pr-2">{row.id}</td>
                        <td className="py-2 pr-2">
                          {editing ? (
                            <input
                              value={editingTurno.name}
                              onChange={(e) => setEditingTurno({ ...editingTurno, name: e.target.value })}
                              className="w-full rounded-md border border-slate-300 px-2 py-1"
                            />
                          ) : (
                            row.name
                          )}
                        </td>
                        <td className="py-2 pr-2">
                          {editing ? (
                            <input
                              type="time"
                              value={editingTurno.horaInicio?.slice(0, 5) ?? ''}
                              onChange={(e) => setEditingTurno({ ...editingTurno, horaInicio: e.target.value })}
                              className="rounded-md border border-slate-300 px-2 py-1"
                            />
                          ) : (
                            row.horaInicio
                          )}
                        </td>
                        <td className="py-2 pr-2">
                          {editing ? (
                            <input
                              type="time"
                              value={editingTurno.horaFinal?.slice(0, 5) ?? ''}
                              onChange={(e) => setEditingTurno({ ...editingTurno, horaFinal: e.target.value })}
                              className="rounded-md border border-slate-300 px-2 py-1"
                            />
                          ) : (
                            row.horaFinal
                          )}
                        </td>
                        <td className="py-2 text-right">
                          <div className="inline-flex gap-1">
                            {editing ? (
                              <>
                                <button type="button" onClick={onSaveTurno} className="rounded p-1 text-emerald-700 hover:bg-emerald-50">
                                  <Save className="h-4 w-4" />
                                </button>
                                <button type="button" onClick={() => setEditingTurno(null)} className="rounded p-1 text-slate-600 hover:bg-slate-100">
                                  <X className="h-4 w-4" />
                                </button>
                              </>
                            ) : (
                              <>
                                <button type="button" onClick={() => setEditingTurno(row)} className="rounded p-1 text-blue-700 hover:bg-blue-50">
                                  <Pencil className="h-4 w-4" />
                                </button>
                                <button type="button" onClick={() => removeItem(`/api/turnos/${row.id}`, 'Turno removido')} className="rounded p-1 text-red-700 hover:bg-red-50">
                                  <Trash2 className="h-4 w-4" />
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>
          <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <div className="mb-3 flex items-center gap-2">
              <Cog className="h-4 w-4 text-emerald-600" />
              <h3 className="font-semibold text-slate-900">Machine</h3>
            </div>
            <form onSubmit={onCreateMachine} className="mb-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
              <input
                value={newMachineName}
                onChange={(e) => setNewMachineName(e.target.value)}
                placeholder="Nome"
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
              <select
                value={newMachineDeviceId}
                onChange={(e) => setNewMachineDeviceId(e.target.value ? Number(e.target.value) : '')}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              >
                <option value="">Selecione o device</option>
                {devices.map((device) => (
                  <option key={device.id} value={device.id}>
                    {device.mnemonic} - {device.name} ({device.id})
                  </option>
                ))}
              </select>
              <select
                value={newMachineSetorId}
                onChange={(e) => setNewMachineSetorId(e.target.value ? Number(e.target.value) : '')}
                className="rounded-md border border-slate-300 px-3 py-2 text-sm sm:col-span-2"
              >
                <option value="">Selecione o setor</option>
                {setores.map((setor) => (
                  <option key={setor.id} value={setor.id}>
                    {setor.name} ({setor.id})
                  </option>
                ))}
              </select>
              <button disabled={isSaving} className="rounded-md bg-emerald-600 px-3 py-2 text-sm font-medium text-white sm:col-span-2">
                Criar
              </button>
            </form>
            <div className="overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="text-left text-slate-600">
                    <th className="pb-2 pr-2">ID</th>
                    <th className="pb-2 pr-2">Nome</th>
                    <th className="pb-2 pr-2">Device ID</th>
                    <th className="pb-2 pr-2">Setor</th>
                    <th className="pb-2 text-right">Acoes</th>
                  </tr>
                </thead>
                <tbody>
                  {machines.map((row, index) => {
                    const editing = editingMachine?.id === row.id;
                    return (
                      <tr key={row.id} className={`border-t border-slate-200 ${index % 2 === 0 ? 'bg-white' : 'bg-slate-200'} hover:bg-blue-100`}>
                        <td className="py-2 pr-2">{row.id}</td>
                        <td className="py-2 pr-2">
                          {editing ? (
                            <input
                              value={editingMachine.name}
                              onChange={(e) => setEditingMachine({ ...editingMachine, name: e.target.value })}
                              className="w-full rounded-md border border-slate-300 px-2 py-1"
                            />
                          ) : (
                            row.name
                          )}
                        </td>
                        <td className="py-2 pr-2">
                          {editing ? (
                            <select
                              value={editingMachine.deviceId}
                              onChange={(e) => setEditingMachine({ ...editingMachine, deviceId: Number(e.target.value) })}
                              className="rounded-md border border-slate-300 px-2 py-1"
                            >
                              {devices.map((device) => (
                                <option key={device.id} value={device.id}>
                                  {device.mnemonic} - {device.name} ({device.id})
                                </option>
                              ))}
                            </select>
                          ) : (
                            `${deviceById.get(row.deviceId) ?? 'ID'} (${row.deviceId})`
                          )}
                        </td>
                        <td className="py-2 pr-2">
                          {editing ? (
                            <select
                              value={editingMachine.setorId}
                              onChange={(e) => setEditingMachine({ ...editingMachine, setorId: Number(e.target.value) })}
                              className="rounded-md border border-slate-300 px-2 py-1"
                            >
                              {setores.map((setor) => (
                                <option key={setor.id} value={setor.id}>
                                  {setor.name} ({setor.id})
                                </option>
                              ))}
                            </select>
                          ) : (
                            `${setorById.get(row.setorId) ?? 'ID'} (${row.setorId})`
                          )}
                        </td>
                        <td className="py-2 text-right">
                          <div className="inline-flex gap-1">
                            {editing ? (
                              <>
                                <button type="button" onClick={onSaveMachine} className="rounded p-1 text-emerald-700 hover:bg-emerald-50">
                                  <Save className="h-4 w-4" />
                                </button>
                                <button type="button" onClick={() => setEditingMachine(null)} className="rounded p-1 text-slate-600 hover:bg-slate-100">
                                  <X className="h-4 w-4" />
                                </button>
                              </>
                            ) : (
                              <>
                                <button type="button" onClick={() => setEditingMachine(row)} className="rounded p-1 text-blue-700 hover:bg-blue-50">
                                  <Pencil className="h-4 w-4" />
                                </button>
                                <button type="button" onClick={() => removeItem(`/api/machines/${row.id}`, 'Machine removida')} className="rounded p-1 text-red-700 hover:bg-red-50">
                                  <Trash2 className="h-4 w-4" />
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

