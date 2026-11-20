import type { ReactNode } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from '@/lib/auth';
import { AppShell } from '@/components/AppShell';
import { LoginPage } from '@/pages/LoginPage';
import { OverviewPage } from '@/pages/OverviewPage';
import { StatementsPage } from '@/pages/StatementsPage';
import { StatementDetailPage } from '@/pages/StatementDetailPage';
import { DisputesPage } from '@/pages/DisputesPage';
import { OrdersPage } from '@/pages/OrdersPage';
import { CouriersPage } from '@/pages/CouriersPage';

function RequireAuth({ children }: { children: ReactNode }) {
  const { session } = useAuth();
  const location = useLocation();
  return session ? <>{children}</> : <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth><AppShell /></RequireAuth>}>
        <Route index element={<OverviewPage />} />
        <Route path="statements" element={<StatementsPage />} />
        <Route path="statements/:id" element={<StatementDetailPage />} />
        <Route path="disputes" element={<DisputesPage />} />
        <Route path="orders" element={<OrdersPage />} />
        <Route path="couriers" element={<CouriersPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
