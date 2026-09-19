import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { WorkOrder, WorkOrderStatus } from '../types';

const COLUMNS: WorkOrderStatus[] = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CLOSED'];
const COLUMN_LABELS: Record<WorkOrderStatus, string> = {
  NEW: 'New', ASSIGNED: 'Assigned', IN_PROGRESS: 'In progress', ON_HOLD: 'On hold',
  COMPLETED: 'Completed', CLOSED: 'Closed', CANCELLED: 'Cancelled',
};

function priorityPillClass(p: string) {
  if (p === 'CRITICAL') return 'pill pill-critical';
  if (p === 'HIGH') return 'pill pill-high';
  if (p === 'MEDIUM') return 'pill pill-medium';
  return 'pill pill-low';
}

export default function WorkOrderBoard() {
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');

  function load() {
    api.get('/api/work-orders', { params: { size: 200 } })
      .then((res) => setOrders(res.data.content))
      .catch(() => setError('Could not load work orders.'));
  }

  useEffect(load, []);

  const filtered = orders.filter((o) => {
    const matchesSearch = !search
      || o.title.toLowerCase().includes(search.toLowerCase())
      || o.code.toLowerCase().includes(search.toLowerCase())
      || o.siteName.toLowerCase().includes(search.toLowerCase());
    const matchesPriority = !priorityFilter || o.priority === priorityFilter;
    return matchesSearch && matchesPriority;
  });

  return (
    <>
      <div className="topline">
        <h1>Work order board</h1>
        <Link to="/board/new" className="btn" style={{ textDecoration: 'none' }}>+ New work order</Link>
      </div>

      <div style={{ display: 'flex', gap: 10, marginBottom: 18 }}>
        <input
          placeholder="Search title, code, or site…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ padding: '8px 10px', border: '1px solid var(--line)', borderRadius: 6, flex: 1, maxWidth: 320 }}
        />
        <select
          value={priorityFilter}
          onChange={(e) => setPriorityFilter(e.target.value)}
          style={{ padding: '8px 10px', border: '1px solid var(--line)', borderRadius: 6 }}
        >
          <option value="">All priorities</option>
          <option value="CRITICAL">Critical</option>
          <option value="HIGH">High</option>
          <option value="MEDIUM">Medium</option>
          <option value="LOW">Low</option>
        </select>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="board">
        {COLUMNS.map((status) => {
          const items = filtered.filter((o) => o.status === status);
          return (
            <div className="board-col" key={status}>
              <h3>{COLUMN_LABELS[status]} <span>{items.length}</span></h3>
              {items.map((o) => (
                <Link to={`/board/${o.id}`} className="wo-card" key={o.id}>
                  <div className="wo-code">{o.code}</div>
                  <div className="wo-title">{o.title}</div>
                  <span className={priorityPillClass(o.priority)}>{o.priority}</span>
                  {o.slaBreached && <span className="pill pill-critical" style={{ marginLeft: 6 }}>SLA</span>}
                  {o.assignedToName && (
                    <div style={{ fontSize: '0.75rem', color: 'var(--ink-soft)', marginTop: 6 }}>
                      {o.assignedToName}
                    </div>
                  )}
                </Link>
              ))}
            </div>
          );
        })}
      </div>
    </>
  );
}
