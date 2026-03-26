import { Routes, Route } from 'react-router';
import { MainLayout } from './layout/MainLayout';
import { DashboardPage } from './pages/DashboardPage';
import { AdminCrudScreen } from './components/AdminCrudScreen';
import { PlcTagCrudScreen } from './components/PlcTagCrudScreen';
import MemorySearch from './components/MemorySearch';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="cadastros" element={<AdminCrudScreen />} />
        <Route path="plc-tag" element={<PlcTagCrudScreen />} />
        <Route path="memory-search" element={<MemorySearch />} />
      </Route>
    </Routes>
  );
}
