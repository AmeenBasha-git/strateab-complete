import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchStrategyDetails } from '../api/dashboardService';
import { ArrowLeft, Settings, Play } from 'lucide-react';
import LoadingState from '../components/layout/LoadingState';
import ErrorState from '../components/layout/ErrorState';
import AccountCard from '../components/dashboard/AccountCard';
import StrategyStateCard from '../components/dashboard/StrategyStateCard';
import LiveMathCard from '../components/dashboard/LiveMathCard';

const StrategyDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const strat = await fetchStrategyDetails(id);
        if (!strat) throw new Error('Strategy not found');
        setData(strat);
        setError(null);
      } catch (err) {
        setError(err.message);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, 5000);
    
    return () => clearInterval(interval);
  }, [id]);

  if (error) return <ErrorState error={error} />;
  if (!data) return <LoadingState />;

  return (
    <div>
      <div className="page-header" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <button 
            onClick={() => navigate('/strategies')}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}
          >
            <ArrowLeft size={20} />
          </button>
          <h1 style={{ margin: 0 }}>{data.strategyName}</h1>
          <span className={`status-badge ${data.status === 'ACTIVE' ? 'running' : ''}`} style={{ backgroundColor: data.status === 'OFFLINE' ? 'rgba(255,255,255,0.1)' : undefined, color: data.status === 'OFFLINE' ? 'var(--text-secondary)' : undefined }}>
            {data.status}
          </span>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          {data.status === 'OFFLINE' ? (
            <button className="btn-primary" style={{ backgroundColor: 'var(--accent-green)', color: 'white' }}>Deploy to Live</button>
          ) : (
            <button className="btn-primary" style={{ backgroundColor: 'var(--accent-red)', color: 'white' }}>Halt Strategy</button>
          )}
          <button className="btn-primary" style={{ backgroundColor: 'var(--card-bg)', color: 'var(--text-primary)', border: '1px solid var(--card-border)' }}>
            <Settings size={16} /> Configure
          </button>
        </div>
      </div>

      {data.status === 'ACTIVE' ? (
        <>
          <h3 className="animate-slide-up" style={{ marginBottom: '1rem', color: 'var(--text-primary)' }}>Performance Summary</h3>
          <div className="grid-container animate-slide-up" style={{ gridTemplateColumns: 'repeat(4, 1fr)', animationDelay: '0.1s' }}>
            <div className="card">
              <div className="card-title">Profit Factor</div>
              <div className="card-value">{data.metrics?.profitFactor || '0.00'}</div>
            </div>
            <div className="card">
              <div className="card-title">Win Rate</div>
              <div className="card-value">{data.metrics?.winRate || '0.0%'}</div>
            </div>
            <div className="card">
              <div className="card-title">Max Drawdown</div>
              <div className="card-value">{data.metrics?.maxDrawdown || '0.0%'}</div>
            </div>
            <div className="card">
              <div className="card-title">Total Trades</div>
              <div className="card-value">{data.metrics?.totalTrades || '0'}</div>
              <div className="card-subtitle">
                Longs: {data.metrics?.totalLongs || 0} | Shorts: {data.metrics?.totalShorts || 0}
              </div>
            </div>
          </div>
          
          <div className="grid-container animate-slide-up" style={{ gridTemplateColumns: 'repeat(4, 1fr)', marginTop: '1.5rem', animationDelay: '0.2s' }}>
            <div className="card">
              <div className="card-title">Expectancy (Per Trade)</div>
              <div className="card-value" style={{ color: 'var(--accent-green)' }}>{data.metrics?.expectancy || '$0.00'}</div>
            </div>
            <div className="card">
              <div className="card-title">Average Win</div>
              <div className="card-value">{data.metrics?.averageWin || '$0.00'}</div>
            </div>
            <div className="card">
              <div className="card-title">Average Loss</div>
              <div className="card-value">{data.metrics?.averageLoss || '$0.00'}</div>
            </div>
            <div className="card">
              <div className="card-title">Reward-to-Risk Ratio</div>
              <div className="card-value">{data.metrics?.rewardToRisk || 'N/A'}</div>
            </div>
          </div>

          <h3 style={{ marginTop: '2rem', marginBottom: '1rem', color: 'var(--text-primary)' }}>Live Execution State</h3>
          <div className="grid-container">
            <div className="card" style={{ borderLeft: '4px solid var(--accent-blue)' }}>
              <div className="card-title">Current Position</div>
              <div className="card-value">{data.state?.position || 'NO_POSITION'}</div>
              <div className="card-subtitle">Tracking symbol: {data.symbol}</div>
            </div>
            
            <div className="card" style={{ borderLeft: `4px solid ${data.state?.killSwitchTriggered ? 'var(--accent-red)' : 'var(--accent-green)'}` }}>
              <div className="card-title">Engine Status</div>
              <div className="card-value" style={{ color: data.state?.killSwitchTriggered ? 'var(--accent-red)' : 'var(--accent-green)' }}>
                {data.state?.killSwitchTriggered ? 'HALTED' : 'RUNNING'}
              </div>
              <div className="card-subtitle">
                {data.state?.killSwitchTriggered ? 'Max drawdown limit reached' : 'Evaluating signals in real-time'}
              </div>
            </div>
          </div>
          
          <div className="grid-container">
            <LiveMathCard data={data} />
          </div>
        </>
      ) : (
        <>
          <div className="card animate-slide-up" style={{ marginBottom: '2rem' }}>
            <h3 style={{ marginBottom: '1rem', color: 'var(--text-primary)' }}>Strategy Profile</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem', lineHeight: '1.6' }}>
              {data.description}
            </p>
            
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
              <div>
                <h4 style={{ color: 'var(--accent-blue)', marginBottom: '0.5rem' }}>Entry Rules</h4>
                <ul style={{ paddingLeft: '1.5rem', color: 'var(--text-secondary)', lineHeight: '1.6' }}>
                  {data.entryRules?.map((rule, idx) => (
                    <li key={idx}>{rule}</li>
                  ))}
                </ul>
              </div>
              <div>
                <h4 style={{ color: 'var(--accent-red)', marginBottom: '0.5rem' }}>Exit Rules</h4>
                <ul style={{ paddingLeft: '1.5rem', color: 'var(--text-secondary)', lineHeight: '1.6' }}>
                  {data.exitRules?.map((rule, idx) => (
                    <li key={idx}>{rule}</li>
                  ))}
                </ul>
              </div>
            </div>
          </div>

          <div className="card animate-slide-up" style={{ textAlign: 'center', padding: '3rem 1rem', animationDelay: '0.2s' }}>
            <h3 style={{ color: 'var(--text-primary)', marginBottom: '0.5rem' }}>Strategy is Offline</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
              This strategy is currently in the repository but is not deployed to the live market.
              <br/>Deploy it to start calculating live performance metrics and tracking executions.
            </p>
            <button className="btn-primary" style={{ backgroundColor: 'var(--accent-green)', color: 'white', margin: '0 auto' }}>
              <Play size={16} /> Deploy Strategy Now
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export default StrategyDetails;
