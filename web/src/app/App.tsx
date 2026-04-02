import { Routes, Route } from 'react-router';
import { MainLayout } from './layout/MainLayout';
import { DashboardPage } from './pages/DashboardPage';
import { AdminCrudPage } from './pages/AdminCrud/AdminCrudPage';
import { PlcTagCrudScreen } from './components/PlcTagCrudScreen';
import MemorySearch from './components/MemorySearch';
import { CollectorControlPage } from './pages/CollectorControlPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="cadastros" element={<AdminCrudPage />} />
        <Route path="plc-tag" element={<PlcTagCrudScreen />} />
        <Route path="memory-search" element={<MemorySearch />} />
        <Route path="collector" element={<CollectorControlPage />} />
      </Route>
    </Routes>
  );
}
