import { ReactNode } from 'react';
import { Factory } from 'lucide-react';
import { NavLink, Outlet, useLocation } from 'react-router';

export function MainLayout() {
  const location = useLocation();

  const getSubtitle = () => {
    switch (location.pathname) {
      case '/':
        return 'Monitoramento em tempo real - 3 linhas';
      case '/cadastros':
        return 'Cadastro e manutencao de estrutura';
      case '/plc-tag':
        return 'Cadastro PLC, memory e tags';
      case '/memory-search':
        return 'Busca de Memória';
      default:
        return '';
    }
  };

  return (
    <div className="min-h-screen lg:h-screen bg-gradient-to-br from-gray-50 to-gray-100 flex flex-col overflow-x-hidden">
      <header className="bg-gradient-to-r from-blue-600 to-blue-800 shadow-xl">
        <div className="px-4 sm:px-6 py-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <Factory className="w-7 h-7 text-white" />
              <div>
                <h1 className="text-xl font-bold text-white">Dashboard PB 4</h1>
                <p className="text-blue-100 text-xs">{getSubtitle()}</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <div className="flex items-center rounded-lg border border-white/20 bg-white/10 p-1">
                <NavLink
                  to="/"
                  className={({ isActive }) =>
                    `rounded-md px-3 py-1.5 text-sm font-medium transition ${
                      isActive ? 'bg-white text-blue-700' : 'text-white hover:bg-white/10'
                    }`
                  }
                >
                  Dashboard
                </NavLink>
                <NavLink
                  to="/cadastros"
                  className={({ isActive }) =>
                    `rounded-md px-3 py-1.5 text-sm font-medium transition ${
                      isActive ? 'bg-white text-blue-700' : 'text-white hover:bg-white/10'
                    }`
                  }
                >
                  Cadastros
                </NavLink>
                <NavLink
                  to="/plc-tag"
                  className={({ isActive }) =>
                    `rounded-md px-3 py-1.5 text-sm font-medium transition ${
                      isActive ? 'bg-white text-blue-700' : 'text-white hover:bg-white/10'
                    }`
                  }
                >
                  PLC/TAG
                </NavLink>
                <NavLink
                  to="/memory-search"
                  className={({ isActive }) =>
                    `rounded-md px-3 py-1.5 text-sm font-medium transition ${
                      isActive ? 'bg-white text-blue-700' : 'text-white hover:bg-white/10'
                    }`
                  }
                >
                  Memory Search
                </NavLink>
              </div>
            </div>
          </div>
        </div>
      </header>

      <main className="flex-1 overflow-y-auto p-3 sm:p-4">
        <Outlet />
      </main>
    </div>
  );
}
