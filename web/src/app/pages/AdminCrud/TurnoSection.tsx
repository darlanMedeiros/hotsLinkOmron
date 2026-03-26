import { FormEvent, useState } from 'react';
import { Clock3, Pencil, Save, Trash2, X } from 'lucide-react';
import { Turno, SectionSharedProps } from './types';
import { requestApi } from '../../../services/api';

interface Props extends SectionSharedProps {
  turnos: Turno[];
}

export function TurnoSection({ turnos, isSaving, withMutation, removeItem }: Props) {
  const [newName, setNewName] = useState('');
  const [newHoraInicio, setNewHoraInicio] = useState('');
  const [newHoraFinal, setNewHoraFinal] = useState('');
  const [editing, setEditing] = useState<Turno | null>(null);

  const onCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (!newName.trim() || !newHoraInicio || !newHoraFinal) return;
    await withMutation(async () => {
      await requestApi<Turno>('/api/turnos', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: newName,
          horaInicio: newHoraInicio,
          horaFinal: newHoraFinal,
        }),
      });
      setNewName('');
      setNewHoraInicio('');
      setNewHoraFinal('');
    }, 'Turno criado');
  };

  const onSave = async () => {
    if (!editing) return;
    await withMutation(async () => {
      await requestApi<Turno>(`/api/turnos/${editing.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: editing.name,
          horaInicio: editing.horaInicio,
          horaFinal: editing.horaFinal,
        }),
      });
      setEditing(null);
    }, 'Turno atualizado');
  };

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center gap-2">
        <Clock3 className="h-4 w-4 text-violet-600" />
        <h3 className="font-semibold text-slate-900">Turno</h3>
      </div>
      <form onSubmit={onCreate} className="mb-3 grid grid-cols-1 gap-2 sm:grid-cols-3">
        <input
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          placeholder="Nome do turno"
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
        <input
          type="time"
          value={newHoraInicio}
          onChange={(e) => setNewHoraInicio(e.target.value)}
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
        <input
          type="time"
          value={newHoraFinal}
          onChange={(e) => setNewHoraFinal(e.target.value)}
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
              <th className="pb-2 pr-2">Hora Início</th>
              <th className="pb-2 pr-2">Hora Final</th>
              <th className="pb-2 text-right">Acoes</th>
            </tr>
          </thead>
          <tbody>
            {turnos.map((row, index) => {
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
                      <input
                        type="time"
                        value={editing.horaInicio?.slice(0, 5) ?? ''}
                        onChange={(e) => setEditing({ ...editing, horaInicio: e.target.value })}
                        className="rounded-md border border-slate-300 px-2 py-1"
                      />
                    ) : (
                      row.horaInicio
                    )}
                  </td>
                  <td className="py-2 pr-2">
                    {isEditing ? (
                      <input
                        type="time"
                        value={editing.horaFinal?.slice(0, 5) ?? ''}
                        onChange={(e) => setEditing({ ...editing, horaFinal: e.target.value })}
                        className="rounded-md border border-slate-300 px-2 py-1"
                      />
                    ) : (
                      row.horaFinal
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
  );
}
