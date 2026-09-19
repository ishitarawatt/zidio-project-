import React, { useEffect, useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { DashboardSummary } from '../types';

const STATUS_LABELS: Record<string, string> = {
  NEW: 'New', ASSIGNED: 'Assigned', IN_PROGRESS: 'In progress',
  ON_HOLD: 'On hold', COMPLETED: 'Completed', CLOSED: 'Closed', CANCELLED: 'Cancelled',
};

export default function Dashboard() {
  const { user } = useAuth();
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/api/work-orders/reports/summary')
      .then((res) => setSummary(res.data))
      .catch(() => setError('Could not load the dashboard. Try again in a moment.'));
  }, []);

  return (
    <>
      <div className="topline">
        <h1>Operations dashboard</h1>
        <div className="who">{user?.name}</div>
      </div>

      {error && <div className="error-banner">{error}</div>}
      {!summary && !error && <p style={{ color: 'var(--ink-soft)' }}>Loading…</p>}

      {summary && (
        <>
          <div className="grid-cols">
            <div className="card">
              <div className="stat-num">{summary.overdueCount}</div>
              <div className="stat-label">Work orders overdue against SLA</div>
            </div>
            <div className="card">
              <div className="stat-num">{summary.slaCompliancePercent}%</div>
              <div className="stat-label">SLA compliance (closed jobs)</div>
            </div>
            <div className="card">
              <div className="stat-num">
                {Object.entries(summary.countsByStatus)
                  .filter(([s]) => !['CLOSED', 'CANCELLED'].includes(s))
                  .reduce((a, [, v]) => a + v, 0)}
              </div>
              <div className="stat-label">Open jobs across the board</div>
            </div>
          </div>

          <div className="card" style={{ marginBottom: 16 }}>
            <h3 style={{ marginTop: 0, fontSize: '0.95rem' }}>Work orders by status</h3>
            <div style={{ display: 'flex', gap: 20, flexWrap: 'wrap', marginTop: 14 }}>
              {Object.entries(summary.countsByStatus).map(([status, count]) => (
                <div key={status}>
                  <div className="stat-num" style={{ fontSize: '1.3rem' }}>{count}</div>
                  <div className="stat-label">{STATUS_LABELS[status] ?? status}</div>
                </div>
              ))}
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <div className="card">
              <h3 style={{ marginTop: 0, fontSize: '0.95rem' }}>Open jobs by technician</h3>
              {Object.keys(summary.openCountByTechnician).length === 0 && (
                <p style={{ color: 'var(--ink-soft)', fontSize: '0.85rem' }}>Nothing assigned right now.</p>
              )}
              <table className="table">
                <tbody>
                  {Object.entries(summary.openCountByTechnician).map(([name, count]) => (
                    <tr key={name}><td>{name}</td><td>{count}</td></tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="card">
              <h3 style={{ marginTop: 0, fontSize: '0.95rem' }}>Open jobs by site</h3>
              <table className="table">
                <tbody>
                  {Object.entries(summary.openCountBySite).map(([name, count]) => (
                    <tr key={name}><td>{name}</td><td>{count}</td></tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </>
  );
}
