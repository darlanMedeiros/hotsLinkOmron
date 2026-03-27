import { FormEvent, useMemo, useState } from 'react';
import { Layers3, Pencil, Save, Trash2, X } from 'lucide-react';
import { Fabrica, MiniFabrica, SectionSharedProps, Setor } from './types';
import { requestApi } from '../../../services/api';

interface Props extends SectionSharedProps {
  miniFabricas: MiniFabrica[];
  fabricas: Fabrica[];
  setores: Setor[];
  fabricaById: Map<number, string>;
  setorById: Map<number, string>;
}

export function MiniFabricaSection({
  miniFabricas,
  fabricas,
  setores,
  fabricaById,
  setorById,
  isSaving,
  withMutation,
  removeItem,
}: Props) {
  const [newName, setNewName] = useState('');
  const [newFabricaId, setNewFabricaId] = useState<number | ''>('');
  const [newSetorIds, setNewSetorIds] = useState<number[]>([]);
  const [editing, setEditing] = useState<MiniFabrica | null>(null);

  const sortedSetores = useMemo(
    () => [...setores].sort((a, b) => a.name.localeCompare(b.name)),
    [setores],
  );

  const toggleSetorSelection = (ids: number[], setorId: number) => {
    if (ids.includes(setorId)) {
      return ids.filter((id) => id !== setorId);
    }
    return [...ids, setorId];
  };

  const onCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (!newName.trim() || !newFabricaId) return;
    await withMutation(async () => {
      await requestApi<MiniFabrica>('/api/mini-fabricas', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: newName, fabricaId: Number(newFabricaId), setorIds: newSetorIds }),
      });
      setNewName('');
      setNewFabricaId('');
      setNewSetorIds([]);
    }, 'Mini fabrica criada');
  };

  const onSave = async () => {
    if (!editing) return;
    await withMutation(async () => {
      await requestApi<MiniFabrica>(`/api/mini-fabricas/${editing.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: editing.name,
          fabricaId: editing.fabricaId,
          setorIds: editing.setorIds,
        }),
      });
      setEditing(null);
    }, 'Mini fabrica atualizada');
  };

  const renderSetorList = (ids: number[]) => {
    if (!ids || ids.length === 0) {
      return 'Sem setores';
    }
    return ids
      .map((id) => `${setorById.get(id) ?? 'ID'} (${id})`)
      .join(', ');
  };

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center gap-2">
        <Layers3 className="h-4 w-4 text-indigo-600" />
        <h3 className="font-semibold text-slate-900">Mini Fabrica</h3>
      </div>

      <form onSubmit={onCreate} className="mb-4 grid grid-cols-1 gap-3 lg:grid-cols-2">
        <input
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          placeholder="Nome"
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
        <select
          value={newFabricaId}
          onChange={(e) => setNewFabricaId(e.target.value ? Number(e.target.value) : '')}
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
        >
          <option value="">Selecione a fabrica</option>
          {fabricas.map((f) => (
            <option key={f.id} value={f.id}>
              {f.name} ({f.id})
            </option>
          ))}
        </select>

        <div className="lg:col-span-2">
          <div className="mb-1 flex items-center justify-between">
            <span className="text-sm font-medium text-slate-700">Setores</span>
            <span className="text-xs text-slate-500">{newSetorIds.length} selecionado(s)</span>
          </div>
          <div className="max-h-40 overflow-auto rounded-md border border-slate-300 p-2">
            <div className="grid grid-cols-1 gap-1 sm:grid-cols-2 lg:grid-cols-3">
              {sortedSetores.map((setor) => {
                const checked = newSetorIds.includes(setor.id);
                return (
                  <label
                    key={setor.id}
                    className={`flex cursor-pointer items-center gap-2 rounded px-2 py-1 text-sm transition ${
                      checked ? 'bg-indigo-50 text-indigo-700' : 'hover:bg-slate-100'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={() => setNewSetorIds((prev) => toggleSetorSelection(prev, setor.id))}
                      className="h-4 w-4 accent-indigo-600"
                    />
                    <span>
                      {setor.name} ({setor.id})
                    </span>
                  </label>
                );
              })}
            </div>
          </div>
        </div>

        <button
          disabled={isSaving}
          className="rounded-md bg-indigo-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-indigo-700 lg:col-span-2"
        >
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
              <th className="pb-2 pr-2">Setores</th>
              <th className="pb-2 text-right">Acoes</th>
            </tr>
          </thead>
          <tbody>
            {miniFabricas.map((row, index) => {
              const isEditing = editing?.id === row.id;
              return (
                <tr
                  key={row.id}
                  className={`border-t border-slate-200 ${index % 2 === 0 ? 'bg-white' : 'bg-slate-200'} hover:bg-blue-100`}
                >
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
                        value={editing.fabricaId}
                        onChange={(e) => setEditing({ ...editing, fabricaId: Number(e.target.value) })}
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
                  <td className="py-2 pr-2">
                    {isEditing ? (
                      <div className="w-[320px] max-w-full rounded-md border border-slate-300 bg-white p-2">
                        <div className="mb-1 text-xs text-slate-500">{editing.setorIds.length} selecionado(s)</div>
                        <div className="grid max-h-28 grid-cols-1 gap-1 overflow-auto sm:grid-cols-2">
                          {sortedSetores.map((setor) => {
                            const checked = editing.setorIds.includes(setor.id);
                            return (
                              <label
                                key={setor.id}
                                className={`flex cursor-pointer items-center gap-2 rounded px-2 py-1 text-xs transition ${
                                  checked ? 'bg-indigo-50 text-indigo-700' : 'hover:bg-slate-100'
                                }`}
                              >
                                <input
                                  type="checkbox"
                                  checked={checked}
                                  onChange={() =>
                                    setEditing({
                                      ...editing,
                                      setorIds: toggleSetorSelection(editing.setorIds, setor.id),
                                    })
                                  }
                                  className="h-3.5 w-3.5 accent-indigo-600"
                                />
                                <span>
                                  {setor.name} ({setor.id})
                                </span>
                              </label>
                            );
                          })}
                        </div>
                      </div>
                    ) : (
                      renderSetorList(row.setorIds)
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
                          <button
                            type="button"
                            onClick={() => removeItem(`/api/mini-fabricas/${row.id}`, 'Mini fabrica removida')}
                            className="rounded p-1 text-red-700 hover:bg-red-50"
                          >
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
