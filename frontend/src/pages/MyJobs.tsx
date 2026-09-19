import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { WorkOrder } from '../types';

export default function MyJobs() {
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/api/work-orders', { params: { size: 100 } })
      .then((res) => setOrders(res.data.content))
      .catch(() => setError('Could not load your jobs.'));
  }, []);

  return (
    <>
      <div className="topline"><h1>My jobs</h1></div>
      {error && <div className="error-banner">{error}</div>}
      {orders.length === 0 && !error && <p style={{ color: 'var(--ink-soft)' }}>Nothing assigned to you right now.</p>}
      <div style={{ display: 'grid', gap: 10 }}>
        {orders.map((o) => (
          <Link to={`/job/${o.id}`} className="card" key={o.id} style={{ textDecoration: 'none', color: 'var(--ink)' }}>
            <div className="wo-code">{o.code} · {o.status}</div>
            <div className="wo-title">{o.title}</div>
            <div style={{ fontSize: '0.85rem', color: 'var(--ink-soft)' }}>{o.siteName}</div>
          </Link>
        ))}
      </div>
    </>
  );
}
