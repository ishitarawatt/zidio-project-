import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import type { WorkOrder, WorkOrderStatus } from '../types';

interface HistoryRow {
  id: number;
  fromStatus: WorkOrderStatus | null;
  toStatus: WorkOrderStatus;
  changedAt: string;
  note: string | null;
}

interface Technician { id: number; name: string; role: string; }

const NEXT_STEPS: Record<WorkOrderStatus, WorkOrderStatus[]> = {
  NEW: ['ASSIGNED', 'CANCELLED'],
  ASSIGNED: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['ON_HOLD', 'COMPLETED'],
  ON_HOLD: ['IN_PROGRESS'],
  COMPLETED: ['CLOSED', 'IN_PROGRESS'],
  CLOSED: [],
  CANCELLED: [],
};

export default function WorkOrderDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const [wo, setWo] = useState<WorkOrder | null>(null);
  const [history, setHistory] = useState<HistoryRow[]>([]);
  const [technicians, setTechnicians] = useState<Technician[]>([]);
  const [error, setError] = useState('');
  const [minutes, setMinutes] = useState('');
  const [note, setNote] = useState('');

  function load() {
    api.get(`/api/work-orders/${id}`).then((res) => setWo(res.data)).catch(() => setError('Could not load this work order.'));
    api.get(`/api/work-orders/${id}/history`).then((res) => setHistory(res.data));
  }

  useEffect(load, [id]);

  useEffect(() => {
    if (user?.role === 'DISPATCHER' || user?.role === 'MANAGER') {
      api.get('/api/users', { params: { role: 'TECHNICIAN' } })
        .then((res) => setTechnicians(res.data))
        .catch(() => setError('Could not load technicians.'));
    }
  }, [user]);

  async function transition(toStatus: WorkOrderStatus) {
    setError('');
    try {
      await api.post(`/api/work-orders/${id}/status`, { toStatus });
      load();
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'That transition was rejected.');
    }
  }

  async function assign(technicianId: number) {
    setError('');
    try {
      await api.post(`/api/work-orders/${id}/assign`, { technicianId });
      load();
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Could not assign this job.');
    }
  }

  async function submitTime(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await api.post(`/api/work-orders/${id}/time`, { minutes: Number(minutes), note });
      setMinutes(''); setNote('');
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Could not log time.');
    }
  }

  if (!wo) return <p style={{ color: 'var(--ink-soft)' }}>{error || 'Loading…'}</p>;

  const backLink = user?.role === 'TECHNICIAN' ? '/my-jobs' : user?.role === 'CUSTOMER' ? '/requests' : '/board';

  return (
    <>
      <Link to={backLink} style={{ fontSize: '0.85rem', color: 'var(--ink-soft)' }}>&larr; Back</Link>
      <div className="topline" style={{ marginTop: 8 }}>
        <h1>{wo.title}</h1>
        <div className="who wo-code">{wo.code}</div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 20 }}>
        <div>
          <div className="card" style={{ marginBottom: 16 }}>
            <p style={{ color: 'var(--ink-soft)', marginTop: 0 }}>{wo.description || 'No description provided.'}</p>
            <p><strong>Site:</strong> {wo.siteName} · <strong>Customer:</strong> {wo.customerName}</p>
            <p><strong>Status:</strong> {wo.status} · <strong>Priority:</strong> {wo.priority}</p>
            {wo.slaDueAt && (
              <p>
                <strong>SLA due:</strong> {new Date(wo.slaDueAt).toLocaleString()}
                {wo.slaBreached && <span className="pill pill-critical" style={{ marginLeft: 8 }}>SLA BREACHED</span>}
              </p>
            )}
            {wo.assignedToName && <p><strong>Assigned to:</strong> {wo.assignedToName}</p>}

            {(user?.role !== 'CUSTOMER') && NEXT_STEPS[wo.status].length > 0 && (
              <div style={{ marginTop: 14, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {NEXT_STEPS[wo.status].map((s) => (
                  <button key={s} className="btn-outline" onClick={() => transition(s)}>
                    Move to {s.replace('_', ' ').toLowerCase()}
                  </button>
                ))}
              </div>
            )}

            {(user?.role === 'DISPATCHER' || user?.role === 'MANAGER') && (
              <div style={{ marginTop: 14 }}>
                <label style={{ fontSize: '0.85rem', color: 'var(--ink-soft)' }}>Assign to</label>
                <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
                  {technicians.map((t) => (
                    <button key={t.id} className="btn-outline" onClick={() => assign(t.id)}>{t.name}</button>
                  ))}
                </div>
              </div>
            )}
          </div>

          {user?.role === 'TECHNICIAN' && wo.assignedToName && (
            <div className="card">
              <h3 style={{ marginTop: 0, fontSize: '0.95rem' }}>Log time</h3>
              <form onSubmit={submitTime} style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
                <div className="field" style={{ marginBottom: 0, width: 100 }}>
                  <label>Minutes</label>
                  <input type="number" min={1} value={minutes} onChange={(e) => setMinutes(e.target.value)} required />
                </div>
                <div className="field" style={{ marginBottom: 0, flex: 1 }}>
                  <label>Note</label>
                  <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Optional" />
                </div>
                <button className="btn">Log</button>
              </form>
            </div>
          )}
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0, fontSize: '0.95rem' }}>Status history</h3>
          {history.map((h) => (
            <div className="history-row" key={h.id}>
              <time>{new Date(h.changedAt).toLocaleString()}</time>
              <div>
                {h.fromStatus ? `${h.fromStatus} → ${h.toStatus}` : `Created (${h.toStatus})`}
                {h.note && <div style={{ color: 'var(--ink-soft)' }}>{h.note}</div>}
              </div>
            </div>
          ))}
        </div>
      </div>
    </>
  );
}
