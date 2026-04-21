import { useEffect, useState } from 'react';
import { requestApi } from '../services/api';

interface QualidadeDefeito {
  defeitoId: number;
  defeitoName: string;
  value: number;
  amostragem: number;
}

export interface QualidadeHistory {
  id: number;
  machineId: number;
  machineName: string;
  value: number; // Amostragem
  hora: string;
  turnoId: number;
  turnoName: string;
  qualidadeParcial: number;
  defeitos: QualidadeDefeito[];
}

export function useLatestQuality(machineId: number | string) {
  const [data, setData] = useState<QualidadeHistory | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    const fetchData = async () => {
      try {
        const response = await requestApi<QualidadeHistory[]>(`/api/qualidade/historico?machineId=${machineId}`);
        if (active) {
          if (response && response.length > 0) {
            setData(response[0]); // O primeiro é o mais recente devido ao ORDER BY hora DESC
          } else {
            setData(null);
          }
          setError(null);
        }
      } catch (err) {
        if (active) {
          setError(err instanceof Error ? err.message : 'Erro ao buscar qualidade');
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    };

    fetchData();
    
    // Polling a cada 30 segundos para a qualidade (menos frequente que PLC)
    const interval = setInterval(fetchData, 30000);

    return () => {
      active = false;
      clearInterval(interval);
    };
  }, [machineId]);

  return { data, isLoading, error };
}
