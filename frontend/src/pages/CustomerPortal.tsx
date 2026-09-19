import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import type { WorkOrder } from '../types';

export default function CustomerPortal() {
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/api/work-orders', { params: { size: 100 } })
      .then((res) => setOrders(res.data.content))
      .catch(() => setError('Could not load your requests.'));
  }, []);

  return (
    <>
      <div className="topline">
        <h1>My requests</h1>
        <Link to="/requests/new" className="btn" style={{ textDecoration: 'none' }}>+ Raise a request</Link>
      </div>
      {error && <div className="error-banner">{error}</div>}
      <table className="table">
        <thead>
          <tr><th>Code</th><th>Title</th><th>Site</th><th>Status</th><th>Raised</th></tr>
        </thead>
        <tbody>
          {orders.map((o) => (
            <tr key={o.id}>
              <td className="wo-code">{o.code}</td>
              <td><Link to={`/job/${o.id}`}>{o.title}</Link></td>
              <td>{o.siteName}</td>
              <td>{o.status}</td>
              <td>{new Date(o.createdAt).toLocaleDateString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}
