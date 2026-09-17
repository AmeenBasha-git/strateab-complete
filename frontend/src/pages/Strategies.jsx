import React, { useState, useEffect, useCallback } from 'react';
import { fetchLibraryStrategies } from '../api/dashboardService';
import LoadingState from '../components/layout/LoadingState';
import CreateStrategyModal from '../components/CreateStrategyModal';
import { Search, Filter, Plus, Play, Pause, Code } from 'lucide-react';

import { useNavigate } from 'react-router-dom';

const Strategies = () => {
  const [library, setLibrary] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const navigate = useNavigate();

  const loadLibrary = useCallback(async () => {
    try {
      setLoading(true);
      const data = await fetchLibraryStrategies();
      const sortedData = data.sort((a, b) => {
        if (a.status === 'ACTIVE' && b.status !== 'ACTIVE') return -1;
        if (a.status !== 'ACTIVE' && b.status === 'ACTIVE') return 1;
        return 0;
      });
      setLibrary(sortedData);
    } catch (err) {
      console.error("Failed to load library", err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadLibrary();
  }, [loadLibrary]);

  return (
    <div>
      <div className="page-header">
        <h1>Strategy Library</h1>
        <button className="btn-primary" onClick={() => setShowCreateModal(true)}>
          <Plus size={16} /> Create New Strategy
        </button>
      </div>

      <div className="table-container animate-slide-up">
        <div className="table-header-row" style={{ backgroundColor: 'var(--card-bg)' }}>
          <div style={{ display: 'flex', gap: '1rem', width: '100%' }}>
            <div style={{ flexGrow: 1, position: 'relative' }}>
              <Search size={16} color="var(--text-muted)" style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)' }} />
              <input 
                type="text" 
                placeholder="Search algorithms..." 
                style={{ width: '100%', padding: '0.6rem 1rem 0.6rem 2.5rem', border: '1px solid var(--card-border)', borderRadius: '6px', outline: 'none' }} 
              />
            </div>
            <button style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0 1rem', background: 'rgba(255,255,255,0.02)', border: '1px solid var(--card-border)', borderRadius: '6px', cursor: 'pointer', color: 'var(--text-secondary)' }}>
              <Filter size={16} /> Filters
            </button>
          </div>
        </div>
        
        {loading ? <LoadingState /> : (
          <table>
            <thead style={{ backgroundColor: 'rgba(255,255,255,0.02)' }}>
              <tr>
                <th>Strategy Name</th>
                <th>Type</th>
                <th>Version</th>
                <th>Symbol</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {library.map((strat) => (
                <tr 
                  key={strat.strategyName} 
                  onClick={() => navigate(`/strategies/${strat.strategyName}`)}
                  style={{ cursor: 'pointer' }}
                >
                  <td style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{strat.strategyName}</td>
                  <td style={{ color: 'var(--text-secondary)' }}>
                    {strat.strategyType === 'USER' ? (
                      <span style={{
                        background: 'rgba(139, 92, 246, 0.15)',
                        color: '#a78bfa',
                        padding: '2px 8px',
                        borderRadius: '4px',
                        fontSize: '0.75rem',
                        fontWeight: 600,
                      }}>USER</span>
                    ) : strat.type}
                  </td>
                  <td style={{ fontFamily: 'monospace' }}>{strat.version}</td>
                  <td>{strat.symbol || 'ANY'}</td>
                  <td>
                    <span className={`status-badge ${strat.status === 'ACTIVE' ? 'running' : ''}`} style={{ backgroundColor: strat.status === 'OFFLINE' ? 'rgba(255,255,255,0.1)' : undefined, color: strat.status === 'OFFLINE' ? 'var(--text-secondary)' : undefined }}>
                      {strat.status}
                    </span>
                  </td>
                  <td style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                    {strat.status === 'OFFLINE' ? (
                      <button style={{ padding: '0.25rem 0.5rem', background: 'rgba(16, 185, 129, 0.1)', color: 'var(--accent-green)', border: '1px solid rgba(16, 185, 129, 0.2)', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.75rem', fontWeight: 600 }}>
                        <Play size={12} /> Deploy
                      </button>
                    ) : (
                      <button style={{ padding: '0.25rem 0.5rem', background: 'rgba(244, 63, 94, 0.1)', color: 'var(--accent-red)', border: '1px solid rgba(244, 63, 94, 0.2)', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.75rem', fontWeight: 600 }}>
                        <Pause size={12} /> Halt
                      </button>
                    )}
                    <button style={{ padding: '0.25rem 0.5rem', background: 'transparent', border: '1px solid var(--card-border)', borderRadius: '4px', cursor: 'pointer', color: 'var(--text-secondary)' }}>
                      <Code size={14} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <CreateStrategyModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onCreated={loadLibrary}
      />
    </div>
  );
};

export default Strategies;
