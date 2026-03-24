import React, { useState, useEffect } from 'react';

interface Device {
  id: number;
  name: string;
  mnemonic: string;
}

interface MemoryValueByDeviceDTO {
  deviceId: number;
  plcMnemonic: string;
  tagName: string;
  memoryName: string;
  value: number;
  timestamp: string;
}

export const MemorySearch: React.FC = () => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedMnemonic, setSelectedMnemonic] = useState<string>('');
  const [memoryValues, setMemoryValues] = useState<MemoryValueByDeviceDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch('/api/devices')
      .then(res => res.json())
      .then(data => setDevices(data))
      .catch(err => setError('Erro ao carregar devices: ' + err.message));
  }, []);

  useEffect(() => {
    if (!selectedMnemonic) {
      setMemoryValues([]);
      return;
    }

    setLoading(true);
    setError(null);

    fetch(`/api/devices/${selectedMnemonic}/memory-values`)
      .then(res => {
        if (res.status === 204) return [];
        if (!res.ok) throw new Error('Nenhum dado encontrado.');
        return res.json();
      })
      .then(data => setMemoryValues(data))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [selectedMnemonic]);

  return (
    <div>
      <h2>Pesquisa de Memórias por Device</h2>

      {error && <p style={{ color: 'red' }}>{error}</p>}

      <label htmlFor="device-select">Selecione um Device: </label>
      <select
        id="device-select"
        value={selectedMnemonic}
        onChange={e => setSelectedMnemonic(e.target.value)}
      >
        <option value="">-- Selecione --</option>
        {devices.map(device => (
          <option key={device.id} value={device.mnemonic}>
            {device.name} ({device.mnemonic})
          </option>
        ))}
      </select>

      {loading && <p>Carregando dados...</p>}
      {!loading && selectedMnemonic && memoryValues.length === 0 && !error && (
        <p>Nenhuma memória encontrada para esse device.</p>
      )}

      {!loading && memoryValues.length > 0 && (
        <table className="min-w-full divide-y divide-gray-200 border border-gray-300 rounded-md mt-4">
          <thead className="bg-gray-100 sticky top-0">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Tag Name</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Memory Name</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Value</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">Timestamp</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {memoryValues.map(mv => (
              <tr key={`${mv.deviceId}-${mv.tagName}-${mv.memoryName}`} className="hover:bg-gray-50">
                <td className="px-6 py-2 whitespace-nowrap text-sm text-gray-900">{mv.tagName}</td>
                <td className="px-6 py-2 whitespace-nowrap text-sm text-gray-900">{mv.memoryName}</td>
                <td className="px-6 py-2 whitespace-nowrap text-sm text-gray-900">{mv.value}</td>
                <td className="px-6 py-2 whitespace-nowrap text-sm text-gray-900">{new Date(mv.timestamp).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default MemorySearch;