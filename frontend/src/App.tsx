import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import Shell from './components/Shell';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import WorkOrderBoard from './pages/WorkOrderBoard';
import NewWorkOrder from './pages/NewWorkOrder';
import WorkOrderDetail from './pages/WorkOrderDetail';
import MyJobs from './pages/MyJobs';
import CustomerPortal from './pages/CustomerPortal';
import NewRequest from './pages/NewRequest';

function ProtectedRoute({ children, roles }: { children: React.ReactNode; roles?: string[] }) {
  const { user, loading } = useAuth();
  if (loading) return null;
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return <>{children}</>;
}

export default function App() {
  const { user } = useAuth();

  function homeForRole() {
    if (!user) return '/login';
    if (user.role === 'TECHNICIAN') return '/my-jobs';
    if (user.role === 'CUSTOMER') return '/requests';
    return '/';
  }

  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route element={<ProtectedRoute><Shell /></ProtectedRoute>}>
        <Route
          path="/"
          element={
            <ProtectedRoute roles={['MANAGER', 'DISPATCHER']}>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/board"
          element={
            <ProtectedRoute roles={['MANAGER', 'DISPATCHER']}>
              <WorkOrderBoard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/board/new"
          element={
            <ProtectedRoute roles={['MANAGER', 'DISPATCHER']}>
              <NewWorkOrder />
            </ProtectedRoute>
          }
        />
        <Route path="/board/:id" element={<WorkOrderDetail />} />
        <Route path="/job/:id" element={<WorkOrderDetail />} />

        <Route
          path="/my-jobs"
          element={
            <ProtectedRoute roles={['TECHNICIAN']}>
              <MyJobs />
            </ProtectedRoute>
          }
        />

        <Route
          path="/requests"
          element={
            <ProtectedRoute roles={['CUSTOMER']}>
              <CustomerPortal />
            </ProtectedRoute>
          }
        />
        <Route
          path="/requests/new"
          element={
            <ProtectedRoute roles={['CUSTOMER']}>
              <NewRequest />
            </ProtectedRoute>
          }
        />
      </Route>

      <Route path="*" element={<Navigate to={homeForRole()} replace />} />
    </Routes>
  );
}
