import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';

interface Site { id: number; name: string; }

// Customers raise requests for their own organisation only; the server
// re-checks this even though the UI never offers another customer's id.
export default function NewRequest() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [sites, setSites] = useState<Site[]>([]);
  const [siteId, setSiteId] = useState<number | ''>('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [error, setError] = useState('');
  const customerId = user?.customerId ?? null;

  useEffect(() => {
    if (!customerId) return;
    api.get(`/api/customers/${customerId}/sites`).then((res) => setSites(res.data));
  }, [customerId]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    try {
      const res = await api.post('/api/work-orders', {
        title, description, priority, customerId, siteId,
      });
      navigate(`/job/${res.data.id}`);
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Could not submit your request.');
    }
  }

  return (
    <>
      <div className="topline"><h1>Raise a request</h1></div>
      <div className="card" style={{ maxWidth: 520 }}>
        {error && <div className="error-banner">{error}</div>}
        <form onSubmit={onSubmit}>
          <div className="field">
            <label>Site</label>
            <select value={siteId} onChange={(e) => setSiteId(Number(e.target.value))} required>
              <option value="">Select a site…</option>
              {sites.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div className="field">
            <label>What's the problem?</label>
            <input value={title} onChange={(e) => setTitle(e.target.value)} required />
          </div>
          <div className="field">
            <label>Details</label>
            <textarea rows={4} value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
          <div className="field">
            <label>How urgent is this?</label>
            <select value={priority} onChange={(e) => setPriority(e.target.value)}>
              <option value="LOW">Low - whenever convenient</option>
              <option value="MEDIUM">Medium - within a few days</option>
              <option value="HIGH">High - within 24 hours</option>
              <option value="CRITICAL">Critical - urgent, safety or major disruption</option>
            </select>
          </div>
          <button className="btn">Submit request</button>
        </form>
      </div>
    </>
  );
}
