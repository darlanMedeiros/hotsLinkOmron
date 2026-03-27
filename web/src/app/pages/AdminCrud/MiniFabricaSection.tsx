import { FormEvent, useState } from 'react';
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

  const parseSelectedSetores = (selectedOptions: HTMLOptionsCollection) => {
    const ids: number[] = [];
    for (let i = 0; i < selectedOptions.length; i += 1) {
      const option = selectedOptions.item(i);
      if (option?.selected && option.value) {
        ids.push(Number(option.value));
      }
    }
    return ids;
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
      <form onSubmit={onCreate} className="mb-3 grid grid-cols-1 gap-2 sm:grid-cols-3">
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
        <select
          multiple
          value={newSetorIds.map(String)}
          onChange={(e) => setNewSetorIds(parseSelectedSetores(e.target.options))}
          className="min-h-[92px] rounded-md border border-slate-300 px-2 py-2 text-sm"
        >
          {setores.map((setor) => (
            <option key={setor.id} value={setor.id}>
              {setor.name} ({setor.id})
            </option>
          ))}
        </select>
        <p className="text-xs text-slate-500 sm:col-span-3">Use Ctrl/Cmd para selecionar varios setores.</p>
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
              <th className="pb-2 pr-2">Setores</th>
              <th className="pb-2 text-right">Acoes</th>
            </tr>
          </thead>
          <tbody>
            {miniFabricas.map((row, index) => {
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
                      <select
                        multiple
                        value={editing.setorIds.map(String)}
                        onChange={(e) => setEditing({ ...editing, setorIds: parseSelectedSetores(e.target.options) })}
                        className="min-h-[88px] rounded-md border border-slate-300 px-2 py-1"
                      >
                        {setores.map((setor) => (
                          <option key={setor.id} value={setor.id}>
                            {setor.name} ({setor.id})
                          </option>
                        ))}
                      </select>
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
  );
}
