import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchDashboardStatus, fetchBacktestRunCount } from '../api/dashboardService';
import { formatCurrency } from '../utils/formatters';
import LoadingState from '../components/layout/LoadingState';
import ErrorState from '../components/layout/ErrorState';
import KpiCard from '../components/dashboard/KpiCard';
import LiveMathCard from '../components/dashboard/LiveMathCard';
import StrategyStateCard from '../components/dashboard/StrategyStateCard';

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

  const activeStrategies = strategies.filter(s => s.isActive);
  const runningCount = activeStrategies.filter(s => !s.isKillSwitchTriggered).length;
  const globalEquity = strategies.length > 0 ? strategies[0].accountEquity : 0;

  return (
    <div>
      <div className="page-header">
        <h1>Dashboard Overview</h1>
        <button className="btn-primary">Deploy Strategy</button>
      </div>

      {/* KPI Cards */}
      <div className="grid-container animate-slide-up" style={{ gridTemplateColumns: 'repeat(4, 1fr)', animationDelay: '0.1s' }}>
        <KpiCard
          title="Total Account Equity"
          value={formatCurrency(globalEquity)}
          subtitle="Live Alpaca Balance"
        />
        <KpiCard
          title="Running Strategies"
          value={runningCount.toString()}
          subtitle={`${strategies.length} total registered`}
        />
        <KpiCard
          title="Active Deployments"
          value={activeStrategies.length.toString()}
          subtitle="Strategies enabled"
        />
        <KpiCard
          title="Total Backtests"
          value={backtestCount.toString()}
          subtitle="Historical runs"
        />
      </div>

      {/* Per-strategy detail cards for active strategies with live data */}
      {activeStrategies.map((strat) => (
        <div key={strat.strategyName} style={{ marginBottom: '3rem' }}>
          <h2 className="animate-slide-up" style={{
            color: 'var(--accent-cyan)',
            fontSize: '1.3rem',
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '2px',
            marginBottom: '1.5rem',
            animationDelay: '0.15s'
          }}>
            {strat.strategyName} — {strat.symbol}
          </h2>

          <div className="grid-container animate-slide-up" style={{ gridTemplateColumns: 'repeat(3, 1fr)', animationDelay: '0.2s' }}>
            <StrategyStateCard
              isHalted={strat.isKillSwitchTriggered}
              symbol={strat.symbol}
              positionState={strat.positionState || strat.position || 'N/A'}
            />

            <div className="card">
              <h2 className="card-title">Position Details</h2>
              <div style={{display: 'flex', flexDirection: 'column', gap: '1rem'}}>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                  <span className="math-label">Position Size</span>
                  <span className="math-value" style={{fontSize: '1.1rem'}}>{strat.positionSize ?? 0} shares</span>
                </div>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                  <span className="math-label">Entry Price</span>
                  <span className="math-value data-pulse" style={{fontSize: '1.1rem'}}>{formatCurrency(strat.entryPrice)}</span>
                </div>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                  <span className="math-label">Stop Loss</span>
                  <span className="math-value" style={{fontSize: '1.1rem', color: 'var(--accent-rose)'}}>{formatCurrency(strat.stopLoss)}</span>
                </div>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                  <span className="math-label">Take Profit</span>
                  <span className="math-value" style={{fontSize: '1.1rem', color: 'var(--accent-emerald)'}}>{formatCurrency(strat.takeProfit)}</span>
                </div>
              </div>
            </div>

            {strat.metrics && (
              <div className="card">
                <h2 className="card-title">Performance Metrics</h2>
                <div style={{display: 'flex', flexDirection: 'column', gap: '1rem'}}>
                  <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <span className="math-label">Total Trades</span>
                    <span className="math-value" style={{fontSize: '1.1rem'}}>{strat.metrics.totalTrades}</span>
                  </div>
                  <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <span className="math-label">Win Rate</span>
                    <span className="math-value" style={{fontSize: '1.1rem', color: 'var(--accent-emerald)'}}>{strat.metrics.winRate}</span>
                  </div>
                  <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <span className="math-label">Profit Factor</span>
                    <span className="math-value" style={{fontSize: '1.1rem', color: 'var(--accent-cyan)'}}>{strat.metrics.profitFactor}</span>
                  </div>
                  <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <span className="math-label">Expectancy</span>
                    <span className="math-value" style={{fontSize: '1.1rem'}}>{strat.metrics.expectancy}</span>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* VWAP Bands card — only show when strategy has band data */}
          {(strat.vwap != null || strat.upperBand != null) && (
            <LiveMathCard data={{
              currentPrice: strat.currentPrice,
              upperBand: strat.upperBand,
              vwap: strat.vwap,
              lowerBand: strat.lowerBand
            }} />
          )}
        </div>
      ))}

      {/* Strategy Table — all strategies */}
      <div className="table-container animate-slide-up" style={{ animationDelay: '0.3s' }}>
        <div className="table-header-row">
          <h3 style={{ margin: 0 }}>All Strategies</h3>
        </div>
        <table>
          <thead>
            <tr>
              <th>Strategy Name</th>
              <th>Symbol</th>
              <th>Active</th>
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
                style={{ cursor: 'pointer', opacity: strat.isActive ? 1 : 0.5 }}
              >
                <td style={{ fontWeight: 600, color: 'var(--accent-cyan)' }}>{strat.strategyName}</td>
                <td>{strat.symbol}</td>
                <td>
                  <span className={`status-badge ${strat.isActive ? 'running' : ''}`}>
                    {strat.isActive ? 'ENABLED' : 'DISABLED'}
                  </span>
                </td>
                <td>
                  <span className={`status-badge ${strat.isKillSwitchTriggered ? 'halted' : strat.isActive ? 'running' : ''}`}>
                    {strat.isKillSwitchTriggered ? 'HALTED' : strat.isActive ? 'RUNNING' : 'IDLE'}
                  </span>
                </td>
                <td>
                  <span className={`status-badge ${(strat.positionState || strat.position) === 'LONG' ? 'long' : (strat.positionState || strat.position) === 'SHORT' ? 'short' : ''}`}>
                    {strat.positionState || strat.position || 'N/A'}
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
