import { FormEvent, useState } from 'react';
import { AlertCircle, Pencil, Save, Trash2, X } from 'lucide-react';
import { Defeito, SectionSharedProps } from './types';
import { requestApi } from '../../../services/api';

interface Props extends SectionSharedProps {
  defeitos: Defeito[];
}

export function DefeitoSection({ defeitos, isSaving, withMutation, removeItem }: Props) {
  const [newName, setNewName] = useState('');
  const [newNumber, setNewNumber] = useState<string>('');
  const [editing, setEditing] = useState<Defeito | null>(null);

  const onCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;
    await withMutation(async () => {
      await requestApi<Defeito>('/api/defeitos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          name: newName,
          number: newNumber ? parseInt(newNumber, 10) : null
        }),
      });
      setNewName('');
      setNewNumber('');
    }, 'Defeito criado');
  };

  const onSave = async () => {
    if (!editing) return;
    await withMutation(async () => {
      await requestApi<Defeito>(`/api/defeitos/${editing.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          name: editing.name,
          number: editing.number
        }),
      });
      setEditing(null);
    }, 'Defeito atualizado');
  };

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center gap-2">
        <AlertCircle className="h-4 w-4 text-red-600" />
        <h3 className="font-semibold text-slate-900">Defeitos</h3>
      </div>
      <form onSubmit={onCreate} className="mb-3 flex flex-wrap gap-2">
        <input
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          placeholder="Nome do defeito"
          className="flex-1 min-w-[200px] rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
        <input
          type="number"
          value={newNumber}
          onChange={(e) => setNewNumber(e.target.value)}
          placeholder="Numero ID"
          className="w-32 rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
        <button disabled={isSaving} className="rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-blue-700 disabled:opacity-50">
          Criar
        </button>
      </form>
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead>
            <tr className="text-left text-slate-600">
              <th className="pb-2 pr-2">ID</th>
              <th className="pb-2 pr-2">Nome</th>
              <th className="pb-2 pr-2">Numero</th>
              <th className="pb-2 text-right">Acoes</th>
            </tr>
          </thead>
          <tbody>
            {defeitos.map((row, index) => {
              const isEditing = editing?.id === row.id;
              return (
                <tr key={row.id} className={`border-t border-slate-200 ${index % 2 === 0 ? 'bg-white' : 'bg-slate-50/50'} hover:bg-blue-50/50 transition`}>
                  <td className="py-2 pr-2 text-slate-500">{row.id}</td>
                  <td className="py-2 pr-2">
                    {isEditing ? (
                      <input
                        value={editing.name}
                        onChange={(e) => setEditing({ ...editing, name: e.target.value })}
                        className="w-full rounded-md border border-slate-300 px-2 py-1"
                      />
                    ) : (
                      <span className="font-medium text-slate-700">{row.name}</span>
                    )}
                  </td>
                  <td className="py-2 pr-2">
                    {isEditing ? (
                      <input
                        type="number"
                        value={editing.number ?? ''}
                        onChange={(e) => setEditing({ ...editing, number: e.target.value ? parseInt(e.target.value, 10) : null })}
                        className="w-full rounded-md border border-slate-300 px-2 py-1"
                      />
                    ) : (
                      <span className="text-slate-600">{row.number ?? '-'}</span>
                    )}
                  </td>
                  <td className="py-2 text-right">
                    <div className="inline-flex gap-1">
                      {isEditing ? (
                        <>
                          <button type="button" onClick={onSave} className="rounded p-1 text-emerald-700 hover:bg-emerald-50 transition">
                            <Save className="h-4 w-4" />
                          </button>
                          <button type="button" onClick={() => setEditing(null)} className="rounded p-1 text-slate-600 hover:bg-slate-100 transition">
                            <X className="h-4 w-4" />
                          </button>
                        </>
                      ) : (
                        <>
                          <button type="button" onClick={() => setEditing(row)} className="rounded p-1 text-blue-700 hover:bg-blue-50 transition">
                            <Pencil className="h-4 w-4" />
                          </button>
                          <button type="button" onClick={() => removeItem(`/api/defeitos/${row.id}`, 'Defeito removido')} className="rounded p-1 text-red-700 hover:bg-red-50 transition">
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
            {defeitos.length === 0 && (
              <tr>
                <td colSpan={4} className="py-8 text-center text-slate-400 italic">
                  Nenhum defeito cadastrado
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
