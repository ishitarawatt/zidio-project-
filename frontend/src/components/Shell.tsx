import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

export default function Shell() {
  const { user, logout } = useAuth();
  if (!user) return null;

  const links: { to: string; label: string; roles: string[] }[] = [
    { to: '/', label: 'Dashboard', roles: ['MANAGER', 'DISPATCHER'] },
    { to: '/board', label: 'Work order board', roles: ['MANAGER', 'DISPATCHER'] },
    { to: '/my-jobs', label: 'My jobs', roles: ['TECHNICIAN'] },
    { to: '/requests', label: 'My requests', roles: ['CUSTOMER'] },
  ];

  return (
    <div className="app-shell">
      <nav className="rail">
        <div className="rail-brand">KEYSTONE <small>MERIDIAN FSM</small></div>
        {links.filter((l) => l.roles.includes(user.role)).map((l) => (
          <NavLink key={l.to} to={l.to} end={l.to === '/'}>{l.label}</NavLink>
        ))}
        <div className="rail-footer">
          {user.name} · {user.role}
          <br />
          <button onClick={logout}>Sign out</button>
        </div>
      </nav>
      <div className="main">
        <Outlet />
      </div>
    </div>
  );
}
