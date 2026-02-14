import { Factory, Activity } from 'lucide-react';
import { ProductionLine } from './components/ProductionLine';
import { Defect } from './components/DefectsList';

export default function App() {
  // Dados mockados para Linha 1
  const defectsLine1: Defect[] = [
    {
      id: 1,
      tipo: 'Trinca Superficial',
      descricao: 'Detectada trinca na superfície da peça durante inspeção visual',
      quantidade: 12,
      timestamp: '14/02/2026 14:32',
      severidade: 'alta'
    },
    {
      id: 2,
      tipo: 'Dimensão Fora do Padrão',
      descricao: 'Espessura da peça 0.5mm abaixo da especificação',
      quantidade: 8,
      timestamp: '14/02/2026 13:15',
      severidade: 'media'
    },
    {
      id: 3,
      tipo: 'Acabamento Irregular',
      descricao: 'Rugosidade acima do limite aceitável na zona de contato',
      quantidade: 5,
      timestamp: '14/02/2026 11:47',
      severidade: 'baixa'
    }
  ];

  // Dados mockados para Linha 2
  const defectsLine2: Defect[] = [
    {
      id: 4,
      tipo: 'Desalinhamento',
      descricao: 'Peça apresentou desalinhamento de 2mm no eixo central',
      quantidade: 15,
      timestamp: '14/02/2026 14:18',
      severidade: 'alta'
    },
    {
      id: 5,
      tipo: 'Porosidade Excessiva',
      descricao: 'Material apresenta porosidade acima de 3% do volume',
      quantidade: 6,
      timestamp: '14/02/2026 12:45',
      severidade: 'media'
    },
    {
      id: 6,
      tipo: 'Marca de Ferramenta',
      descricao: 'Marcas visíveis de ferramenta na superfície acabada',
      quantidade: 3,
      timestamp: '14/02/2026 10:22',
      severidade: 'baixa'
    }
  ];

  // Dados mockados para Linha 3
  const defectsLine3: Defect[] = [
    {
      id: 7,
      tipo: 'Deformação Plástica',
      descricao: 'Peça apresentou deformação plástica durante processo de prensagem',
      quantidade: 18,
      timestamp: '14/02/2026 15:03',
      severidade: 'alta'
    },
    {
      id: 8,
      tipo: 'Rebarbas Excessivas',
      descricao: 'Rebarbas além do limite de tolerância nas bordas',
      quantidade: 11,
      timestamp: '14/02/2026 13:28',
      severidade: 'media'
    },
    {
      id: 9,
      tipo: 'Coloração Irregular',
      descricao: 'Variação de coloração indica aquecimento não uniforme',
      quantidade: 4,
      timestamp: '14/02/2026 11:15',
      severidade: 'baixa'
    }
  ];

  return (
    <div className="h-screen bg-gradient-to-br from-gray-50 to-gray-100 flex flex-col overflow-hidden">
      {/* Header */}
      <header className="bg-gradient-to-r from-blue-600 to-blue-800 shadow-xl">
        <div className="px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Factory className="w-7 h-7 text-white" />
              <div>
                <h1 className="text-xl font-bold text-white">Dashboard Industrial</h1>
                <p className="text-blue-100 text-xs">Monitoramento em tempo real - 3 Linhas de Produção</p>
              </div>
            </div>
            <div className="flex items-center gap-2 bg-white/10 backdrop-blur-sm px-3 py-1.5 rounded-lg">
              <Activity className="w-4 h-4 text-green-300 animate-pulse" />
              <span className="text-white text-sm font-medium">Sistema Ativo</span>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content - Grid de 3 colunas */}
      <main className="flex-1 overflow-hidden p-4">
        <div className="grid grid-cols-3 gap-4 h-full">
          {/* Linha de Produção 1 */}
          <ProductionLine
            lineNumber={1}
            lineName="Estamparia - Setor A"
            pecasPrensa={1542}
            pecasRoller={1305}
            aprovadas={2822}
            rejeitadas={25}
            trendPrensa={8}
            trendRoller={-3}
            defects={defectsLine1}
            color="bg-gradient-to-r from-blue-500 to-blue-600"
          />

          {/* Linha de Produção 2 */}
          <ProductionLine
            lineNumber={2}
            lineName="Usinagem - Setor B"
            pecasPrensa={1823}
            pecasRoller={1567}
            aprovadas={3366}
            rejeitadas={24}
            trendPrensa={12}
            trendRoller={5}
            defects={defectsLine2}
            color="bg-gradient-to-r from-green-500 to-green-600"
          />

          {/* Linha de Produção 3 */}
          <ProductionLine
            lineNumber={3}
            lineName="Montagem - Setor C"
            pecasPrensa={1398}
            pecasRoller={1124}
            aprovadas={2489}
            rejeitadas={33}
            trendPrensa={-5}
            trendRoller={2}
            defects={defectsLine3}
            color="bg-gradient-to-r from-orange-500 to-orange-600"
          />
        </div>
      </main>
    </div>
  );
}