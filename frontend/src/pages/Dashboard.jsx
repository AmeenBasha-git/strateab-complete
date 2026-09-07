import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchDashboardStatus, fetchBacktestRunCount } from '../api/dashboardService';
import { formatCurrency } from '../utils/formatters';
import LoadingState from '../components/layout/LoadingState';
import ErrorState from '../components/layout/ErrorState';
import KpiCard from '../components/dashboard/KpiCard';

const Dashboard = () => {
  const [strategies, setStrategies] = useState(null);
  const [backtestCount, setBacktestCount] = useState(0);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchData = async () => {
      try {
        const json = await fetchDashboardStatus();
        setStrategies(json);
        
        // Also fetch backtest count
        try {
          const countRes = await fetchBacktestRunCount();
          if (countRes && countRes.data !== undefined) {
             setBacktestCount(countRes.data);
          }
        } catch (e) {
          console.warn('Could not load backtest count', e);
        }
        
        setError(null);
      } catch (err) {
        setError(err.message);
      }
    };

    fetchData(); 
    const interval = setInterval(fetchData, 5000); 
    
    return () => clearInterval(interval);
  }, []);

  if (error) return <ErrorState error={error} />;
  if (!strategies) return <LoadingState />;

  const runningCount = strategies.filter(s => !s.isKillSwitchTriggered).length;
  // Use the first strategy's equity as global account equity for now
  const globalEquity = strategies.length > 0 ? strategies[0].accountEquity : 0;

  return (
    <div>
      <div className="page-header">
        <h1>Dashboard Overview</h1>
        <button className="btn-primary">Deploy Strategy</button>
      </div>

      {/* 4 KPI Cards */}
      <div className="grid-container animate-slide-up" style={{ gridTemplateColumns: 'repeat(4, 1fr)', animationDelay: '0.1s' }}>
        <KpiCard 
          title="Total Account Equity" 
          value={formatCurrency(globalEquity)} 
          subtitle="Live Alpaca Balance" 
        />
        <KpiCard 
          title="Running Strategies" 
          value={runningCount.toString()} 
          subtitle="Active algorithms" 
        />
        <KpiCard 
          title="Active Deployments" 
          value="1" 
          subtitle="Production servers" 
        />
        <KpiCard 
          title="Total Backtests" 
          value={backtestCount.toString()} 
          subtitle="Historical runs" 
        />
      </div>

      {/* Strategy Table */}
      <div className="table-container animate-slide-up" style={{ animationDelay: '0.3s' }}>
        <div className="table-header-row">
          <h3 style={{ margin: 0 }}>Active Strategies</h3>
        </div>
        <table>
          <thead>
            <tr>
              <th>Strategy Name</th>
              <th>Symbol</th>
              <th>Status</th>
              <th>Current Position</th>
              <th>Current Price</th>
            </tr>
          </thead>
          <tbody>
            {strategies.map((strat) => (
              <tr 
                key={strat.strategyName} 
                onClick={() => navigate(`/strategies/${strat.strategyName}`)}
                style={{ cursor: 'pointer' }}
              >
                <td style={{ fontWeight: 600, color: 'var(--accent-blue)' }}>{strat.strategyName}</td>
                <td>{strat.symbol}</td>
                <td>
                  <span className={`status-badge ${strat.isKillSwitchTriggered ? 'halted' : 'running'}`}>
                    {strat.isKillSwitchTriggered ? 'HALTED' : 'RUNNING'}
                  </span>
                </td>
                <td>
                  <span className={`status-badge ${strat.positionState === 'LONG' ? 'long' : strat.positionState === 'SHORT' ? 'short' : ''}`}>
                    {strat.positionState}
                  </span>
                </td>
                <td style={{ fontFamily: 'monospace' }}>{formatCurrency(strat.currentPrice)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default Dashboard;
