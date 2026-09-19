import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';

interface Customer { id: number; name: string; }
interface Site { id: number; name: string; }

export default function NewWorkOrder() {
  const navigate = useNavigate();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [sites, setSites] = useState<Site[]>([]);
  const [customerId, setCustomerId] = useState<number | ''>('');
  const [siteId, setSiteId] = useState<number | ''>('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState('MEDIUM');
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/api/customers', { params: { size: 100 } }).then((res) => setCustomers(res.data.content));
  }, []);

  useEffect(() => {
    if (!customerId) { setSites([]); return; }
    api.get(`/api/customers/${customerId}/sites`).then((res) => setSites(res.data));
  }, [customerId]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    try {
      const res = await api.post('/api/work-orders', {
        title, description, priority, customerId, siteId,
      });
      navigate(`/board/${res.data.id}`);
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Could not create the work order.');
    }
  }

  return (
    <>
      <div className="topline"><h1>New work order</h1></div>
      <div className="card" style={{ maxWidth: 520 }}>
        {error && <div className="error-banner">{error}</div>}
        <form onSubmit={onSubmit}>
          <div className="field">
            <label>Customer</label>
            <select value={customerId} onChange={(e) => setCustomerId(Number(e.target.value))} required>
              <option value="">Select a customer…</option>
              {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </div>
          <div className="field">
            <label>Site</label>
            <select value={siteId} onChange={(e) => setSiteId(Number(e.target.value))} required disabled={!customerId}>
              <option value="">Select a site…</option>
              {sites.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div className="field">
            <label>Title</label>
            <input value={title} onChange={(e) => setTitle(e.target.value)} required />
          </div>
          <div className="field">
            <label>Description</label>
            <textarea rows={4} value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
          <div className="field">
            <label>Priority</label>
            <select value={priority} onChange={(e) => setPriority(e.target.value)}>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>
          <button className="btn">Create work order</button>
        </form>
      </div>
    </>
  );
}
