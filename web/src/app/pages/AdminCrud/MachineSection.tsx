import { FormEvent, useMemo, useState } from 'react';
import { Cog, Pencil, Save, Trash2, X } from 'lucide-react';
import { Device, Machine, MiniFabrica, Setor, SectionSharedProps } from './types';
import { requestApi } from '../../../services/api';

interface Props extends SectionSharedProps {
  machines: Machine[];
  devices: Device[];
  miniFabricas: MiniFabrica[];
  setores: Setor[];
  deviceById: Map<number, string>;
  miniFabricaById: Map<number, string>;
  setorById: Map<number, string>;
}

export function MachineSection({
  machines,
  devices,
  miniFabricas,
  setores,
  deviceById,
  miniFabricaById,
  setorById,
  isSaving,
  withMutation,
  removeItem,
}: Props) {
  const [newName, setNewName] = useState('');
  const [newDeviceId, setNewDeviceId] = useState<number | ''>('');
  const [newMiniFabricaId, setNewMiniFabricaId] = useState<number | ''>('');
  const [newSetorId, setNewSetorId] = useState<number | ''>('');
  const [editing, setEditing] = useState<Machine | null>(null);

  const availableSetoresForNewMachine = useMemo(() => {
    if (!newMiniFabricaId) {
      return [] as Setor[];
    }
    const mf = miniFabricas.find((row) => row.id === Number(newMiniFabricaId));
    if (!mf) {
      return [] as Setor[];
    }
    return setores.filter((setor) => mf.setorIds.includes(setor.id));
  }, [miniFabricas, setores, newMiniFabricaId]);

  const availableSetoresForEditing = useMemo(() => {
    if (!editing?.miniFabricaId) {
      return [] as Setor[];
    }
    const mf = miniFabricas.find((row) => row.id === editing.miniFabricaId);
    if (!mf) {
      return [] as Setor[];
    }
    return setores.filter((setor) => mf.setorIds.includes(setor.id));
  }, [miniFabricas, setores, editing]);

  const onCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (!newName.trim() || !newDeviceId || !newMiniFabricaId || !newSetorId) return;
    await withMutation(async () => {
      await requestApi<Machine>('/api/machines', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: newName,
          deviceId: Number(newDeviceId),
          miniFabricaId: Number(newMiniFabricaId),
          setorId: Number(newSetorId),
        }),
      });
      setNewName('');
      setNewDeviceId('');
      setNewMiniFabricaId('');
      setNewSetorId('');
    }, 'Machine criada');
  };

  const onSave = async () => {
    if (!editing) return;
    await withMutation(async () => {
      await requestApi<Machine>(`/api/machines/${editing.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: editing.name,
          deviceId: editing.deviceId,
          miniFabricaId: editing.miniFabricaId,
          setorId: editing.setorId,
        }),
      });
      setEditing(null);
    }, 'Machine atualizada');
  };

  const onChangeNewMiniFabrica = (value: string) => {
    setNewMiniFabricaId(value ? Number(value) : '');
    setNewSetorId('');
  };

  const onChangeEditingMiniFabrica = (value: string) => {
    if (!editing) {
      return;
    }
    setEditing({ ...editing, miniFabricaId: Number(value), setorId: 0 });
  };

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center gap-2">
        <Cog className="h-4 w-4 text-emerald-600" />
        <h3 className="font-semibold text-slate-900">Machine</h3>
      </div>
      <form onSubmit={onCreate} className="mb-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
        <input
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          placeholder="Nome"
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
        <select
          value={newDeviceId}
          onChange={(e) => setNewDeviceId(e.target.value ? Number(e.target.value) : '')}
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
          value={newMiniFabricaId}
          onChange={(e) => onChangeNewMiniFabrica(e.target.value)}
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
        >
          <option value="">Selecione a mini fabrica</option>
          {miniFabricas.map((mini) => (
            <option key={mini.id} value={mini.id}>
              {mini.name} ({mini.id})
            </option>
          ))}
        </select>
        <select
          value={newSetorId}
          onChange={(e) => setNewSetorId(e.target.value ? Number(e.target.value) : '')}
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          disabled={!newMiniFabricaId}
        >
          <option value="">Selecione o setor</option>
          {availableSetoresForNewMachine.map((setor) => (
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
              <th className="pb-2 pr-2">Device</th>
              <th className="pb-2 pr-2">Mini Fabrica</th>
              <th className="pb-2 pr-2">Setor</th>
              <th className="pb-2 text-right">Acoes</th>
            </tr>
          </thead>
          <tbody>
            {machines.map((row, index) => {
              const isEditing = editing?.id === row.id;
              return (
                <tr key={row.id} className={`border-t border-slate-200 ${index % 2 === 0 ? 'bg-white' : 'bg-slate-200'} hover:bg-blue-100`}>
                  <td className="py-2 pr-2">{row.id}</td>
                  <td className="py-2 pr-2">
                    {isEditing ? (
                      <input
                        value={editing.name}
                        onChange={(e) => setEditing({ ...editing, name: e.target.value })}
                        className="w-full rounded-md border border-slate-300 px-2 py-1"
                      />
                    ) : (
                      row.name
                    )}
                  </td>
                  <td className="py-2 pr-2">
                    {isEditing ? (
                      <select
                        value={editing.deviceId}
                        onChange={(e) => setEditing({ ...editing, deviceId: Number(e.target.value) })}
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
                    {isEditing ? (
                      <select
                        value={editing.miniFabricaId}
                        onChange={(e) => onChangeEditingMiniFabrica(e.target.value)}
                        className="rounded-md border border-slate-300 px-2 py-1"
                      >
                        {miniFabricas.map((mini) => (
                          <option key={mini.id} value={mini.id}>
                            {mini.name} ({mini.id})
                          </option>
                        ))}
                      </select>
                    ) : (
                      `${miniFabricaById.get(row.miniFabricaId) ?? 'ID'} (${row.miniFabricaId})`
                    )}
                  </td>
                  <td className="py-2 pr-2">
                    {isEditing ? (
                      <select
                        value={editing.setorId}
                        onChange={(e) => setEditing({ ...editing, setorId: Number(e.target.value) })}
                        className="rounded-md border border-slate-300 px-2 py-1"
                      >
                        <option value="0">Selecione o setor</option>
                        {availableSetoresForEditing.map((setor) => (
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
                      {isEditing ? (
                        <>
                          <button type="button" onClick={onSave} className="rounded p-1 text-emerald-700 hover:bg-emerald-50">
                            <Save className="h-4 w-4" />
                          </button>
                          <button type="button" onClick={() => setEditing(null)} className="rounded p-1 text-slate-600 hover:bg-slate-100">
                            <X className="h-4 w-4" />
                          </button>
                        </>
                      ) : (
                        <>
                          <button type="button" onClick={() => setEditing(row)} className="rounded p-1 text-blue-700 hover:bg-blue-50">
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
  );
}
