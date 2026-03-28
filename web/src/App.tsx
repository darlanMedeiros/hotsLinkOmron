import { Routes, Route } from 'react-router';
import { MainLayout } from '@/layouts/MainLayout';
import { DashboardPage } from '@/pages/DashboardPage';
import { AdminCrudPage } from '@/pages/AdminCrudPage';
import { PlcTagCrudScreen } from '@/features/plc-tags/PlcTagCrudScreen';
import MemorySearch from '@/features/memory-search/MemorySearch';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="cadastros" element={<AdminCrudPage />} />
        <Route path="plc-tag" element={<PlcTagCrudScreen />} />
        <Route path="memory-search" element={<MemorySearch />} />
      </Route>
    </Routes>
  );
}
