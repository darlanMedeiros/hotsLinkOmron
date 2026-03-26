import { FormEvent, useState } from 'react';
import { Building2, Pencil, Save, Trash2, X } from 'lucide-react';
import { Fabrica, SectionSharedProps } from './types';
import { requestApi } from '../../../services/api';

interface Props extends SectionSharedProps {
  fabricas: Fabrica[];
}

export function FabricaSection({ fabricas, isSaving, withMutation, removeItem }: Props) {
  const [newName, setNewName] = useState('');
  const [editing, setEditing] = useState<Fabrica | null>(null);

  const onCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;
    await withMutation(async () => {
      await requestApi<Fabrica>('/api/fabricas', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: newName }),
      });
      setNewName('');
    }, 'Fabrica criada');
  };

  const onSave = async () => {
    if (!editing) return;
    await withMutation(async () => {
      await requestApi<Fabrica>(`/api/fabricas/${editing.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: editing.name }),
      });
      setEditing(null);
    }, 'Fabrica atualizada');
  };

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center gap-2">
        <Building2 className="h-4 w-4 text-blue-600" />
        <h3 className="font-semibold text-slate-900">Fabrica</h3>
      </div>
      <form onSubmit={onCreate} className="mb-3 flex gap-2">
        <input
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
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
              const isEditing = editing?.id === row.id;
              return (
                <tr key={row.id} className={`border-t border-slate-200 ${index % 2 === 0 ? 'bg-white' : 'bg-slate-200'} hover:bg-blue-100`}>
                  <td className="py-2 pr-2 text-slate-700">{row.id}</td>
                  <td className="py-2 pr-2">
                    {isEditing ? (
                      <input
                        value={editing.name}
                        onChange={(e) => setEditing({ ...editing, name: e.target.value })}
                        className="w-full rounded-md border border-slate-300 px-2 py-1"
                      />
                    ) : (
                      <span>{row.name}</span>
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
  );
}
