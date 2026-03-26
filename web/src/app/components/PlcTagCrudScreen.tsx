import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Cpu, HardDrive, Tag, Pencil, Save, Trash2, X } from 'lucide-react';

type ViewMessage = {
  type: 'success' | 'error';
  text: string;
};

type Device = {
  id: number;
  mnemonic: string;
  name: string;
  description: string | null;
  nodeId: number | null;
};

type Memory = {
  id: number;
  deviceId: number;
  name: string;
  address: number;
};

type TagCrud = {
  id: number;
  name: string;
  deviceId: number;
  memoryId: number;
  persistHistory: boolean;
};

type SectionKey = 'device' | 'memory' | 'tag';

async function requestApi<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init);
  if (!response.ok) {
    let message = `Erro HTTP ${response.status}`;
    try {
      const body = await response.json() as { message?: string };
      if (body.message) {
        message = body.message;
      }
    } catch (_err) {
      // Keep fallback message for non-json body.
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return await response.json() as T;
}

export function PlcTagCrudScreen() {
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<ViewMessage | null>(null);

  const sections: { key: SectionKey; label: string; icon: JSX.Element }[] = [
    { key: 'device', label: 'Devices', icon: <Cpu className="h-4 w-4 text-blue-600" /> },
    { key: 'memory', label: 'Memories', icon: <HardDrive className="h-4 w-4 text-indigo-600" /> },
    { key: 'tag', label: 'Tags', icon: <Tag className="h-4 w-4 text-emerald-600" /> },
  ];
  const [activeSection, setActiveSection] = useState<SectionKey>('device');

  const [devices, setDevices] = useState<Device[]>([]);
  const [memories, setMemories] = useState<Memory[]>([]);
  const [tags, setTags] = useState<TagCrud[]>([]);

  const [newDeviceMnemonic, setNewDeviceMnemonic] = useState('');
  const [newDeviceName, setNewDeviceName] = useState('');
  const [newDeviceDescription, setNewDeviceDescription] = useState('');
  const [newDeviceNodeId, setNewDeviceNodeId] = useState<number | ''>('');

  const [newMemoryDeviceId, setNewMemoryDeviceId] = useState<number | ''>('');
  const [newMemoryName, setNewMemoryName] = useState('');
  const [newMemoryAddress, setNewMemoryAddress] = useState<number | ''>('');

  const [newTagName, setNewTagName] = useState('');
  const [newTagDeviceId, setNewTagDeviceId] = useState<number | ''>('');
  const [newTagMemoryId, setNewTagMemoryId] = useState<number | ''>('');
  const [newTagPersistHistory, setNewTagPersistHistory] = useState(true);

  const [editingDevice, setEditingDevice] = useState<Device | null>(null);
  const [editingMemory, setEditingMemory] = useState<Memory | null>(null);
  const [editingTag, setEditingTag] = useState<TagCrud | null>(null);

  const availableMemoriesForNewTag = useMemo(() => {
    if (newTagDeviceId === '') {
      return [] as Memory[];
    }
    return memories.filter((m) => m.deviceId === Number(newTagDeviceId));
  }, [memories, newTagDeviceId]);

  const loadAll = async (): Promise<string[]> => {
    const failures: string[] = [];
    const [devicesRes, memoriesRes, tagsRes] = await Promise.allSettled([
      requestApi<Device[]>('/api/devices'),
      requestApi<Memory[]>('/api/memories'),
      requestApi<TagCrud[]>('/api/tags'),
    ]);

    if (devicesRes.status === 'fulfilled') {
      setDevices(devicesRes.value);
    } else {
      failures.push(`Device: ${devicesRes.reason instanceof Error ? devicesRes.reason.message : 'erro'}`);
    }

    if (memoriesRes.status === 'fulfilled') {
      setMemories(memoriesRes.value);
    } else {
      failures.push(`Memory: ${memoriesRes.reason instanceof Error ? memoriesRes.reason.message : 'erro'}`);
    }

    if (tagsRes.status === 'fulfilled') {
      setTags(tagsRes.value);
    } else {
      failures.push(`Tag: ${tagsRes.reason instanceof Error ? tagsRes.reason.message : 'erro'}`);
    }

    return failures;
  };

  useEffect(() => {
    let alive = true;
    const run = async () => {
      try {
        const failures = await loadAll();
        if (alive && failures.length > 0) {
          setMessage({ type: 'error', text: failures.join(' | ') });
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
      const failures = await loadAll();
      if (failures.length > 0) {
        setMessage({ type: 'error', text: `${successText}. Falhas de carga: ${failures.join(' | ')}` });
      } else {
        setMessage({ type: 'success', text: successText });
      }
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

  const deviceLabel = (id: number) => {
    const found = devices.find((d) => d.id === id);
    return found ? `${found.mnemonic} (${id})` : `ID ${id}`;
  };

  const memoryLabel = (id: number) => {
    const found = memories.find((m) => m.id === id);
    return found ? `${found.name} (${id})` : `ID ${id}`;
  };

  const renderDeviceSection = () => (
    <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center gap-2">
        <Cpu className="h-4 w-4 text-blue-600" />
        <h3 className="font-semibold text-slate-900">Device</h3>
      </div>
      <form
        onSubmit={async (e: FormEvent) => {
          e.preventDefault();
          await withMutation(async () => {
            await requestApi<Device>('/api/devices', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                mnemonic: newDeviceMnemonic,
                name: newDeviceName,
                description: newDeviceDescription,
                nodeId: newDeviceNodeId === '' ? null : Number(newDeviceNodeId),
              }),
            });
            setNewDeviceMnemonic('');
            setNewDeviceName('');
            setNewDeviceDescription('');
            setNewDeviceNodeId('');
          }, 'Device criado');
        }}
        className="mb-3 grid grid-cols-1 gap-2 sm:grid-cols-2"
      >
        <input value={newDeviceMnemonic} onChange={(e) => setNewDeviceMnemonic(e.target.value)} placeholder="Mnemonic" className="rounded-md border border-slate-300 px-3 py-2 text-sm" />
        <input value={newDeviceName} onChange={(e) => setNewDeviceName(e.target.value)} placeholder="Nome" className="rounded-md border border-slate-300 px-3 py-2 text-sm" />
        <input value={newDeviceDescription} onChange={(e) => setNewDeviceDescription(e.target.value)} placeholder="Descricao" className="rounded-md border border-slate-300 px-3 py-2 text-sm sm:col-span-2" />
        <input type="number" min={0} value={newDeviceNodeId} onChange={(e) => setNewDeviceNodeId(e.target.value ? Number(e.target.value) : '')} placeholder="No ID" className="rounded-md border border-slate-300 px-3 py-2 text-sm sm:col-span-2" />
        <button disabled={isSaving} className="rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white sm:col-span-2">Criar</button>
      </form>
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead>
            <tr className="text-left text-slate-600">
              <th className="pb-2 pr-2">ID</th>
              <th className="pb-2 pr-2">Mnemonic</th>
              <th className="pb-2 pr-2">Nome</th>
              <th className="pb-2 pr-2">Descricao</th>
              <th className="pb-2 pr-2">No ID</th>
              <th className="pb-2 text-right">Acoes</th>
            </tr>
          </thead>
          <tbody>
            {devices.map((row, index) => {
              const editing = editingDevice?.id === row.id;
              return (
                <tr key={row.id} className={`border-t border-slate-200 ${index % 2 === 0 ? 'bg-white' : 'bg-slate-200'} hover:bg-blue-100`}>
                  <td className="py-2 pr-2">{row.id}</td>
                  <td className="py-2 pr-2">
                    {editing ? <input value={editingDevice.mnemonic} onChange={(e) => setEditingDevice({ ...editingDevice, mnemonic: e.target.value })} className="w-24 rounded border border-slate-300 px-2 py-1" /> : row.mnemonic}
                  </td>
                  <td className="py-2 pr-2">
                    {editing ? <input value={editingDevice.name} onChange={(e) => setEditingDevice({ ...editingDevice, name: e.target.value })} className="w-28 rounded border border-slate-300 px-2 py-1" /> : row.name}
                  </td>
                  <td className="py-2 pr-2">
                    {editing ? <input value={editingDevice.description ?? ''} onChange={(e) => setEditingDevice({ ...editingDevice, description: e.target.value })} className="w-36 rounded border border-slate-300 px-2 py-1" /> : (row.description ?? '')}
                  </td>
                  <td className="py-2 pr-2">
                    {editing ? <input type="number" min={0} value={editingDevice.nodeId ?? ''} onChange={(e) => setEditingDevice({ ...editingDevice, nodeId: e.target.value ? Number(e.target.value) : null })} className="w-24 rounded border border-slate-300 px-2 py-1" /> : (row.nodeId ?? '')}
                  </td>
                  <td className="py-2 text-right">
                    <div className="inline-flex gap-1">
                      {editing ? (
                        <>
                          <button type="button" onClick={async () => { await withMutation(async () => { await requestApi<Device>(`/api/devices/${editingDevice.id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(editingDevice) }); setEditingDevice(null); }, 'Device atualizado'); }} className="rounded p-1 text-emerald-700 hover:bg-emerald-50"><Save className="h-4 w-4" /></button>
                          <button type="button" onClick={() => setEditingDevice(null)} className="rounded p-1 text-slate-600 hover:bg-slate-100"><X className="h-4 w-4" /></button>
                        </>
                      ) : (
                        <>
                          <button type="button" onClick={() => setEditingDevice(row)} className="rounded p-1 text-blue-700 hover:bg-blue-50"><Pencil className="h-4 w-4" /></button>
                          <button type="button" onClick={() => removeItem(`/api/devices/${row.id}`, 'Device removido')} className="rounded p-1 text-red-700 hover:bg-red-50"><Trash2 className="h-4 w-4" /></button>
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

  const renderMemorySection = () => (
    <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center gap-2">
        <HardDrive className="h-4 w-4 text-indigo-600" />
        <h3 className="font-semibold text-slate-900">Memory</h3>
      </div>
      <form
        onSubmit={async (e: FormEvent) => {
          e.preventDefault();
          await withMutation(async () => {
            await requestApi<Memory>('/api/memories', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                deviceId: Number(newMemoryDeviceId),
                name: newMemoryName,
                address: Number(newMemoryAddress),
              }),
            });
            setNewMemoryDeviceId('');
            setNewMemoryName('');
            setNewMemoryAddress('');
          }, 'Memory criada');
        }}
        className="mb-3 grid grid-cols-1 gap-2 sm:grid-cols-3"
      >
        <select value={newMemoryDeviceId} onChange={(e) => setNewMemoryDeviceId(e.target.value ? Number(e.target.value) : '')} className="rounded-md border border-slate-300 px-3 py-2 text-sm">
          <option value="">Selecione Device</option>
          {devices.map((d) => <option key={d.id} value={d.id}>{d.mnemonic} ({d.id})</option>)}
        </select>
        <input value={newMemoryName} onChange={(e) => setNewMemoryName(e.target.value)} placeholder="Nome da memory (ex: DM_29)" className="rounded-md border border-slate-300 px-3 py-2 text-sm" />
        <input type="number" min={0} value={newMemoryAddress} onChange={(e) => setNewMemoryAddress(e.target.value ? Number(e.target.value) : '')} placeholder="Address" className="rounded-md border border-slate-300 px-3 py-2 text-sm" />
        <button disabled={isSaving} className="rounded-md bg-indigo-600 px-3 py-2 text-sm font-medium text-white sm:col-span-3">Criar</button>
      </form>
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead>
            <tr className="text-left text-slate-600">
              <th className="pb-2 pr-2">ID</th>
              <th className="pb-2 pr-2">Device</th>
              <th className="pb-2 pr-2">Nome</th>
              <th className="pb-2 pr-2">Address</th>
              <th className="pb-2 text-right">Acoes</th>
            </tr>
          </thead>
          <tbody>
            {memories.map((row, index) => {
              const editing = editingMemory?.id === row.id;
              return (
                <tr key={row.id} className={`border-t border-slate-200 ${index % 2 === 0 ? 'bg-white' : 'bg-slate-200'} hover:bg-blue-100`}>
                  <td className="py-2 pr-2">{row.id}</td>
                  <td className="py-2 pr-2">
                    {editing ? (
                      <select value={editingMemory.deviceId} onChange={(e) => setEditingMemory({ ...editingMemory, deviceId: Number(e.target.value) })} className="rounded border border-slate-300 px-2 py-1">
                        {devices.map((d) => <option key={d.id} value={d.id}>{d.mnemonic} ({d.id})</option>)}
                      </select>
                    ) : deviceLabel(row.deviceId)}
                  </td>
                  <td className="py-2 pr-2">
                    {editing ? <input value={editingMemory.name} onChange={(e) => setEditingMemory({ ...editingMemory, name: e.target.value })} className="w-36 rounded border border-slate-300 px-2 py-1" /> : row.name}
                  </td>
                  <td className="py-2 pr-2">
                    {editing ? <input type="number" min={0} value={editingMemory.address} onChange={(e) => setEditingMemory({ ...editingMemory, address: Number(e.target.value || 0) })} className="w-24 rounded border border-slate-300 px-2 py-1" /> : row.address}
                  </td>
                  <td className="py-2 text-right">
                    <div className="inline-flex gap-1">
                      {editing ? (
                        <>
                          <button type="button" onClick={async () => { await withMutation(async () => { await requestApi<Memory>(`/api/memories/${editingMemory.id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(editingMemory) }); setEditingMemory(null); }, 'Memory atualizada'); }} className="rounded p-1 text-emerald-700 hover:bg-emerald-50"><Save className="h-4 w-4" /></button>
                          <button type="button" onClick={() => setEditingMemory(null)} className="rounded p-1 text-slate-600 hover:bg-slate-100"><X className="h-4 w-4" /></button>
                        </>
                      ) : (
                        <>
                          <button type="button" onClick={() => setEditingMemory(row)} className="rounded p-1 text-blue-700 hover:bg-blue-50"><Pencil className="h-4 w-4" /></button>
                          <button type="button" onClick={() => removeItem(`/api/memories/${row.id}`, 'Memory removida')} className="rounded p-1 text-red-700 hover:bg-red-50"><Trash2 className="h-4 w-4" /></button>
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

  const renderTagSection = () => (
    <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="mb-3 flex items-center gap-2">
        <Tag className="h-4 w-4 text-emerald-600" />
        <h3 className="font-semibold text-slate-900">Tag</h3>
      </div>
      <form
        onSubmit={async (e: FormEvent) => {
          e.preventDefault();
          await withMutation(async () => {
            await requestApi<TagCrud>('/api/tags', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                name: newTagName,
                deviceId: Number(newTagDeviceId),
                memoryId: Number(newTagMemoryId),
                persistHistory: newTagPersistHistory,
              }),
            });
            setNewTagName('');
            setNewTagDeviceId('');
            setNewTagMemoryId('');
            setNewTagPersistHistory(true);
          }, 'Tag criada');
        }}
        className="mb-3 grid grid-cols-1 gap-2 sm:grid-cols-3"
      >
        <input value={newTagName} onChange={(e) => setNewTagName(e.target.value)} placeholder="Nome da tag" className="rounded-md border border-slate-300 px-3 py-2 text-sm sm:col-span-3" />
        <select
          value={newTagDeviceId}
          onChange={(e) => {
            setNewTagDeviceId(e.target.value ? Number(e.target.value) : '');
            setNewTagMemoryId('');
          }}
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
        >
          <option value="">Device</option>
          {devices.map((d) => <option key={d.id} value={d.id}>{d.mnemonic} ({d.id})</option>)}
        </select>
        <select value={newTagMemoryId} onChange={(e) => setNewTagMemoryId(e.target.value ? Number(e.target.value) : '')} className="rounded-md border border-slate-300 px-3 py-2 text-sm sm:col-span-2">
          <option value="">{newTagDeviceId === '' ? 'Selecione o device antes' : 'Memory'}</option>
          {availableMemoriesForNewTag.map((m) => <option key={m.id} value={m.id}>{m.name} ({m.id})</option>)}
        </select>
        <label className="flex items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-sm sm:col-span-3">
          <input
            type="checkbox"
            checked={newTagPersistHistory}
            onChange={(e) => setNewTagPersistHistory(e.target.checked)}
          />
          Persistir historico (memory_value)
        </label>
        <button disabled={isSaving} className="rounded-md bg-emerald-600 px-3 py-2 text-sm font-medium text-white sm:col-span-3">Criar</button>
      </form>
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead>
            <tr className="text-left text-slate-600">
              <th className="pb-2 pr-2">ID</th>
              <th className="pb-2 pr-2">Nome</th>
              <th className="pb-2 pr-2">Device</th>
              <th className="pb-2 pr-2">Memory</th>
              <th className="pb-2 pr-2">Persist.</th>
              <th className="pb-2 text-right">Acoes</th>
            </tr>
          </thead>
          <tbody>
            {tags.map((row, index) => {
              const editing = editingTag?.id === row.id;
              return (
                <tr key={row.id} className={`border-t border-slate-200 ${index % 2 === 0 ? 'bg-white' : 'bg-slate-200'} hover:bg-blue-100`}>
                  <td className="py-2 pr-2">{row.id}</td>
                  <td className="py-2 pr-2">
                    {editing ? <input value={editingTag.name} onChange={(e) => setEditingTag({ ...editingTag, name: e.target.value })} className="w-36 rounded border border-slate-300 px-2 py-1" /> : row.name}
                  </td>
                  <td className="py-2 pr-2">
                    {editing ? (
                      <select value={editingTag.deviceId} onChange={(e) => setEditingTag({ ...editingTag, deviceId: Number(e.target.value) })} className="rounded border border-slate-300 px-2 py-1">
                        {devices.map((d) => <option key={d.id} value={d.id}>{d.mnemonic} ({d.id})</option>)}
                      </select>
                    ) : deviceLabel(row.deviceId)}
                  </td>
                  <td className="py-2 pr-2">
                    {editing ? (
                      <select value={editingTag.memoryId} onChange={(e) => setEditingTag({ ...editingTag, memoryId: Number(e.target.value) })} className="rounded border border-slate-300 px-2 py-1">
                        {memories
                          .filter((m) => m.deviceId === editingTag.deviceId)
                          .map((m) => <option key={m.id} value={m.id}>{m.name} ({m.id})</option>)}
                      </select>
                    ) : memoryLabel(row.memoryId)}
                  </td>
                  <td className="py-2 pr-2">
                    {editing ? (
                      <label className="flex items-center gap-2">
                        <input
                          type="checkbox"
                          checked={editingTag.persistHistory}
                          onChange={(e) => setEditingTag({ ...editingTag, persistHistory: e.target.checked })}
                        />
                        {editingTag.persistHistory ? 'Historico' : 'Current'}
                      </label>
                    ) : (row.persistHistory ? 'Historico' : 'Current')}
                  </td>
                  <td className="py-2 text-right">
                    <div className="inline-flex gap-1">
                      {editing ? (
                        <>
                          <button type="button" onClick={async () => { await withMutation(async () => { await requestApi<TagCrud>(`/api/tags/${editingTag.id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(editingTag) }); setEditingTag(null); }, 'Tag atualizada'); }} className="rounded p-1 text-emerald-700 hover:bg-emerald-50"><Save className="h-4 w-4" /></button>
                          <button type="button" onClick={() => setEditingTag(null)} className="rounded p-1 text-slate-600 hover:bg-slate-100"><X className="h-4 w-4" /></button>
                        </>
                      ) : (
                        <>
                          <button type="button" onClick={() => setEditingTag(row)} className="rounded p-1 text-blue-700 hover:bg-blue-50"><Pencil className="h-4 w-4" /></button>
                          <button type="button" onClick={() => removeItem(`/api/tags/${row.id}`, 'Tag removida')} className="rounded p-1 text-red-700 hover:bg-red-50"><Trash2 className="h-4 w-4" /></button>
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

  return (
    <div className="space-y-4">
      <div className="rounded-lg border border-slate-200 bg-white px-4 py-3 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Cadastro PLC / TAG</h2>
        <p className="text-sm text-slate-600">
          Gerencie Device, Memory e Tag para configurar o mapa de leitura dos PLCs.
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
          {activeSection === 'device' && renderDeviceSection()}
          {activeSection === 'memory' && renderMemorySection()}
          {activeSection === 'tag' && renderTagSection()}
        </div>
      )}
    </div>
  );
}


