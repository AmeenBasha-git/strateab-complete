import React from 'react';
import { Bell, Moon, Sun, UserCircle } from 'lucide-react';

const TopNav = () => {
  return (
    <div className="top-nav">
      <div></div>
      
      <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
        <div style={{ fontSize: '0.875rem', fontWeight: '500', padding: '0.25rem 0.75rem', backgroundColor: '#f1f5f9', borderRadius: '6px' }}>
          Workspace: Production
        </div>
        <Bell size={20} color="var(--text-secondary)" style={{ cursor: 'pointer' }} />
        <Sun size={20} color="var(--text-secondary)" style={{ cursor: 'pointer' }} />
        <UserCircle size={28} color="var(--text-primary)" style={{ cursor: 'pointer' }} />
      </div>
    </div>
  );
};

export default TopNav;
