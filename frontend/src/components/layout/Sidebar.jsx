import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  LineChart,
  Database,
  History,
  Rocket,
  Activity,
  BarChart2,
  FileText,
  Bell,
  ShieldCheck,
  Settings,
  User,
  LogOut
} from 'lucide-react';

const Sidebar = () => {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const navItems = [
    { name: 'Dashboard', path: '/', icon: <LayoutDashboard size={18} /> },
    { name: 'Strategies', path: '/strategies', icon: <LineChart size={18} /> },
    { name: 'Datasets', path: '/datasets', icon: <Database size={18} /> },
    { name: 'Backtests', path: '/backtests', icon: <History size={18} /> },
    { name: 'Deployments', path: '/deployments', icon: <Rocket size={18} /> },
    { name: 'Live Monitoring', path: '/live', icon: <Activity size={18} /> },
    { name: 'Metrics', path: '/metrics', icon: <BarChart2 size={18} /> },
    { name: 'Reports', path: '/reports', icon: <FileText size={18} /> },
    { name: 'Notifications', path: '/notifications', icon: <Bell size={18} /> },
    { name: 'Administration', path: '/admin', icon: <ShieldCheck size={18} /> },
    { name: 'Settings', path: '/settings', icon: <Settings size={18} /> },
  ];

  return (
    <div className="sidebar">
      <div className="sidebar-header" style={{ color: 'var(--accent-blue)', textShadow: '0 0 10px rgba(6,182,212,0.5)' }}>
        <span style={{ color: 'var(--text-primary)' }}>STRAT</span>LAB
      </div>
      <div className="sidebar-nav">
        {navItems.map((item) => (
          <NavLink 
            key={item.name} 
            to={item.path} 
            className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
          >
            {item.icon}
            {item.name}
          </NavLink>
        ))}
      </div>
      <div className="sidebar-nav" style={{ flexGrow: 0, borderTop: '1px solid var(--card-border)' }}>
        <NavLink to="/profile" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <User size={18} />
          Profile
        </NavLink>
        <button onClick={handleLogout} className="nav-item" style={{
          background: 'none', border: 'none', cursor: 'pointer',
          width: '100%', textAlign: 'left', font: 'inherit',
          color: 'var(--accent-rose)',
        }}>
          <LogOut size={18} />
          Logout
        </button>
      </div>
    </div>
  );
};

export default Sidebar;
