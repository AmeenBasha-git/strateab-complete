import React, { useState, useEffect } from 'react';
import { fetchTrades } from '../api/dashboardService';
import { formatCurrency } from '../utils/formatters';
import KpiCard from '../components/dashboard/KpiCard';
import LoadingState from '../components/layout/LoadingState';

const LiveMonitoring = () => {
  const [trades, setTrades] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const getTrades = async () => {
      try {
        const data = await fetchTrades();
        setTrades(data);
      } catch (err) {
        console.error("Failed to load trades", err);
      } finally {
        setLoading(false);
      }
    };

    getTrades();
    const interval = setInterval(getTrades, 5000); // Auto-refresh trade log every 5s
    
    return () => clearInterval(interval);
  }, []);

  return (
    <div>
      <div className="page-header">
        <h1>Live Monitoring</h1>
        <button className="btn-primary" style={{ backgroundColor: 'var(--card-bg)', color: 'var(--text-primary)', border: '1px solid var(--card-border)' }}>
          System Logs
        </button>
      </div>

      <div className="grid-container" style={{ gridTemplateColumns: 'repeat(4, 1fr)' }}>
        <KpiCard title="Market Data Feed" value="CONNECTED" subtitle="Alpaca WebSocket" />
        <KpiCard title="Execution Engine" value="ACTIVE" subtitle="Running 1 worker" />
        <KpiCard title="CPU Usage" value="12.4%" subtitle="Healthy" />
        <KpiCard title="Memory Usage" value="1.2 GB" subtitle="Healthy" />
      </div>

      <h3 style={{ marginTop: '2rem', marginBottom: '1rem', color: 'var(--text-primary)' }}>Trade Execution Log</h3>
      
      <div className="table-container">
        <div className="table-header-row">
          <h3>Recent Trades (Auto-refreshing)</h3>
        </div>
        
        {loading ? <LoadingState /> : (
          <table>
            <thead>
              <tr>
                <th>Time</th>
                <th>Strategy</th>
                <th>Symbol</th>
                <th>Side</th>
                <th>Entry Price</th>
                <th>Exit Price</th>
                <th>PnL</th>
              </tr>
            </thead>
            <tbody>
              {trades.length === 0 ? (
                <tr>
                  <td colSpan="7" style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '3rem' }}>
                    No trades executed yet today. Waiting for signals...
                  </td>
                </tr>
              ) : (
                trades.map((trade) => (
                  <tr key={trade.id}>
                    <td style={{ color: 'var(--text-secondary)' }}>
                      {new Date(trade.executionTime).toLocaleTimeString()}
                    </td>
                    <td style={{ fontWeight: 500 }}>{trade.strategyName}</td>
                    <td>{trade.symbol}</td>
                    <td>
                      <span className={`status-badge ${trade.direction === 'LONG' ? 'long' : 'short'}`}>
                        {trade.direction}
                      </span>
                    </td>
                    <td style={{ fontFamily: 'monospace' }}>{formatCurrency(trade.entryPrice)}</td>
                    <td style={{ fontFamily: 'monospace' }}>{formatCurrency(trade.exitPrice)}</td>
                    <td style={{ fontFamily: 'monospace', fontWeight: 600, color: trade.pnl > 0 ? 'var(--accent-green)' : 'var(--accent-red)' }}>
                      {trade.pnl > 0 ? '+' : ''}{formatCurrency(trade.pnl)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default LiveMonitoring;
