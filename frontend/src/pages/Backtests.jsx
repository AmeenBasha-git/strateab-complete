import React, { useState, useEffect } from 'react';
import { Play, CheckCircle, XCircle, Clock, TrendingUp, TrendingDown, BarChart2, Activity, Target, DollarSign, ArrowUpDown } from 'lucide-react';
import {
  fetchDatasets, runBacktest,
  fetchBacktestRuns, fetchBacktestStrategies
} from '../api/dashboardService';
import { formatCurrency } from '../utils/formatters';

const Backtests = () => {
  const [datasets, setDatasets] = useState([]);
  const [runs, setRuns] = useState([]);
  const [strategies, setStrategies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [showRunForm, setShowRunForm] = useState(false);
  const [selectedDataset, setSelectedDataset] = useState('');
  const [selectedStrategy, setSelectedStrategy] = useState('');
  const [startingEquity, setStartingEquity] = useState('100000');
  const [running, setRunning] = useState(false);
  const [selectedRun, setSelectedRun] = useState(null);

  useEffect(() => { loadData(); }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [dsRes, runsRes, stratRes] = await Promise.all([
        fetchDatasets(), fetchBacktestRuns(), fetchBacktestStrategies(),
      ]);
      setDatasets(dsRes.data || []);
      setRuns(runsRes.data || []);
      setStrategies(stratRes.data || []);
      setError(null);
    } catch (err) { setError(err.message); }
    finally { setLoading(false); }
  };

  const handleRunBacktest = async (e) => {
    e.preventDefault();
    if (!selectedDataset || !selectedStrategy) return;
    setRunning(true);
    try {
      const res = await runBacktest(selectedDataset, selectedStrategy, parseFloat(startingEquity));
      setShowRunForm(false);
      setSelectedDataset('');
      setSelectedStrategy('');
      if (res.data) setSelectedRun(res.data);
      loadData();
    } catch (err) { setError(err.message); }
    finally { setRunning(false); }
  };

  const statusBadge = (status) => {
    const config = {
      COMPLETED: { className: 'running', icon: <CheckCircle size={12} /> },
      FAILED: { className: 'halted', icon: <XCircle size={12} /> },
      RUNNING: { className: 'running', icon: <Clock size={12} /> },
      PENDING: { className: '', icon: <Clock size={12} /> },
    };
    const c = config[status] || config.PENDING;
    return <span className={`status-badge ${c.className}`} style={{ gap: '0.4rem' }}>{c.icon} {status}</span>;
  };

  if (loading) return <div className="loading-state">Loading backtest data...</div>;

  // --- Metric helpers ---
  const fmt = (v, d = 2) => v != null ? parseFloat(v).toFixed(d) : '—';
  const fmtPct = (v) => v != null ? `${(parseFloat(v) * 100).toFixed(2)}%` : '—';
  const fmtPctRaw = (v) => v != null ? `${parseFloat(v).toFixed(2)}%` : '—';

  const valClass = (v, opts = {}) => {
    if (v == null) return 'neutral';
    const n = parseFloat(v);
    if (n === 0) return 'neutral';
    if (opts.fixed) return opts.fixed;
    const positive = opts.invert ? n < 0 : n > 0;
    return positive ? 'positive' : 'negative';
  };

  const Metric = ({ label, value, cls }) => (
    <div className="metric-cell">
      <div className="metric-label">{label}</div>
      <div className={`metric-value ${cls || 'neutral'}`}>{value}</div>
    </div>
  );

  // --- DETAIL VIEW ---
  if (selectedRun) {
    const r = selectedRun;
    const pnlPositive = r.totalPnl && parseFloat(r.totalPnl) > 0;

    return (
      <div>
        <div className="page-header">
          <h1>Backtest Results</h1>
          <button className="btn-primary" onClick={() => setSelectedRun(null)}>← Back to Runs</button>
        </div>

        {/* Top KPI Cards */}
        <div className="grid-container animate-slide-up" style={{ gridTemplateColumns: 'repeat(4, 1fr)', animationDelay: '0.05s' }}>
          <div className="card">
            <div className="card-title">Strategy</div>
            <div className="card-value" style={{ fontSize: '1.2rem', color: '#22d3ee', wordBreak: 'break-all' }}>{r.strategyName}</div>
            <div className="card-subtitle">{statusBadge(r.status)}</div>
          </div>
          <div className="card">
            <div className="card-title">Total P&L</div>
            <div className="card-value" style={{ color: pnlPositive ? '#34d399' : '#fb7185' }}>
              {pnlPositive ? '+' : ''}{formatCurrency(r.totalPnl)}
            </div>
            <div className="card-subtitle" style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', color: '#a1a1aa' }}>
              {pnlPositive ? <TrendingUp size={14} color="#34d399" /> : <TrendingDown size={14} color="#fb7185" />}
              {pnlPositive ? 'Profitable' : 'Net Loss'}
            </div>
          </div>
          <div className="card">
            <div className="card-title">Final Equity</div>
            <div className="card-value">{formatCurrency(r.finalEquity)}</div>
            <div className="card-subtitle" style={{ color: '#a1a1aa' }}>Started at {formatCurrency(r.startingEquity)}</div>
          </div>
          <div className="card">
            <div className="card-title">Total Trades</div>
            <div className="card-value">{r.totalTrades}</div>
            <div className="card-subtitle" style={{ color: '#a1a1aa' }}>Commission: {r.totalCommission != null ? formatCurrency(r.totalCommission) : '—'}</div>
          </div>
        </div>

        {/* Risk-Adjusted Performance */}
        <div className="metrics-section animate-slide-up" style={{ animationDelay: '0.1s' }}>
          <div className="metrics-section-header">
            <Activity size={16} color="#22d3ee" /> Risk-Adjusted Performance
          </div>
          <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(5, 1fr)' }}>
            <Metric label="Sharpe Ratio" value={fmt(r.sharpeRatio, 4)} cls={valClass(r.sharpeRatio)} />
            <Metric label="Sortino Ratio" value={fmt(r.sortinoRatio, 4)} cls={valClass(r.sortinoRatio)} />
            <Metric label="Calmar Ratio" value={fmt(r.calmarRatio, 4)} cls={valClass(r.calmarRatio)} />
            <Metric label="Annualized Return" value={r.annualizedReturn != null ? fmtPct(r.annualizedReturn) : '—'} cls={valClass(r.annualizedReturn)} />
            <Metric label="Ann. Volatility" value={r.annualizedVolatility != null ? fmtPct(r.annualizedVolatility) : '—'} cls="amber" />
          </div>
        </div>

        {/* Core Performance */}
        <div className="metrics-section animate-slide-up" style={{ animationDelay: '0.15s' }}>
          <div className="metrics-section-header">
            <BarChart2 size={16} color="#a78bfa" /> Core Performance
          </div>
          <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(4, 1fr)' }}>
            <Metric label="Win Rate" value={fmtPctRaw(r.winRate)} cls={r.winRate != null && parseFloat(r.winRate) >= 50 ? 'positive' : 'negative'} />
            <Metric label="Profit Factor" value={fmt(r.profitFactor, 4)} cls={r.profitFactor != null && parseFloat(r.profitFactor) >= 1 ? 'positive' : 'negative'} />
            <Metric label="Max Drawdown" value={r.maxDrawdown != null ? fmtPct(r.maxDrawdown) : '—'} cls="negative" />
            <Metric label="Expectancy / Trade" value={r.expectancy != null ? formatCurrency(r.expectancy) : '—'} cls={valClass(r.expectancy)} />
          </div>
        </div>

        {/* Trade Statistics */}
        <div className="metrics-section animate-slide-up" style={{ animationDelay: '0.2s' }}>
          <div className="metrics-section-header">
            <Target size={16} color="#fbbf24" /> Trade Statistics
          </div>
          <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(4, 1fr)' }}>
            <Metric label="Avg Win" value={r.avgWin != null ? formatCurrency(r.avgWin) : '—'} cls="positive" />
            <Metric label="Avg Loss" value={r.avgLoss != null ? formatCurrency(r.avgLoss) : '—'} cls="negative" />
            <Metric label="Largest Win" value={r.largestWin != null ? formatCurrency(r.largestWin) : '—'} cls="positive" />
            <Metric label="Largest Loss" value={r.largestLoss != null ? formatCurrency(r.largestLoss) : '—'} cls="negative" />
          </div>
          <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(4, 1fr)' }}>
            <Metric label="Risk / Reward" value={fmt(r.riskRewardRatio, 2)} cls="cyan" />
            <Metric label="Max Consec. Wins" value={r.maxConsecutiveWins != null ? r.maxConsecutiveWins : '—'} cls="positive" />
            <Metric label="Max Consec. Losses" value={r.maxConsecutiveLosses != null ? r.maxConsecutiveLosses : '—'} cls="negative" />
            <Metric label="Total Commission" value={r.totalCommission != null ? formatCurrency(r.totalCommission) : '—'} cls="amber" />
          </div>
        </div>

        {/* Directional Breakdown */}
        <div className="metrics-section animate-slide-up" style={{ animationDelay: '0.25s' }}>
          <div className="metrics-section-header">
            <ArrowUpDown size={16} color="#34d399" /> Directional Breakdown
          </div>
          <div className="metrics-grid" style={{ gridTemplateColumns: 'repeat(4, 1fr)' }}>
            <Metric label="Long Trades" value={r.longTrades != null ? r.longTrades : '—'} cls="positive" />
            <Metric label="Long Win Rate" value={fmtPctRaw(r.longWinRate)} cls={r.longWinRate != null && parseFloat(r.longWinRate) >= 50 ? 'positive' : 'negative'} />
            <Metric label="Short Trades" value={r.shortTrades != null ? r.shortTrades : '—'} cls="purple" />
            <Metric label="Short Win Rate" value={fmtPctRaw(r.shortWinRate)} cls={r.shortWinRate != null && parseFloat(r.shortWinRate) >= 50 ? 'positive' : 'negative'} />
          </div>
        </div>

        {/* Equity Curve */}
        {r.equityCurve && r.equityCurve.length > 0 && (
          <div className="equity-curve-card animate-slide-up" style={{ animationDelay: '0.3s' }}>
            <div className="metrics-section-header">
              <DollarSign size={16} color="#22d3ee" /> Equity Curve
            </div>
            <div style={{ position: 'relative', height: '280px', padding: '1.5rem 1.5rem 2rem' }}>
              {(() => {
                const data = r.equityCurve;
                const equities = data.map(p => parseFloat(p.equity));
                const minEq = Math.min(...equities);
                const maxEq = Math.max(...equities);
                const range = maxEq - minEq || 1;
                const h = 240;
                const points = data.map((p, i) => {
                  const x = (i / (data.length - 1)) * 100;
                  const y = h - ((parseFloat(p.equity) - minEq) / range) * (h - 20);
                  return `${x},${y}`;
                }).join(' ');
                const endEq = equities[equities.length - 1];
                const startEq = equities[0];
                const isUp = endEq >= startEq;
                const lineColor = isUp ? '#34d399' : '#fb7185';
                const fillColor = isUp ? 'rgba(52, 211, 153, 0.15)' : 'rgba(251, 113, 133, 0.15)';
                return (
                  <>
                    <svg viewBox={`0 0 100 ${h}`} preserveAspectRatio="none" style={{ width: '100%', height: '100%', display: 'block' }}>
                      <defs>
                        <linearGradient id="eqFill" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor={lineColor} stopOpacity="0.25" />
                          <stop offset="100%" stopColor={lineColor} stopOpacity="0.01" />
                        </linearGradient>
                      </defs>
                      <polygon points={`0,${h} ${points} 100,${h}`} fill="url(#eqFill)" />
                      <polyline points={points} fill="none" stroke={lineColor} strokeWidth="0.5" vectorEffect="non-scaling-stroke" style={{ filter: `drop-shadow(0 0 6px ${lineColor})` }} />
                    </svg>
                    <div style={{ position: 'absolute', top: '1rem', left: '1.5rem', display: 'flex', gap: '2rem' }}>
                      <span style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: '0.85rem', color: '#d4d4d8' }}>
                        Peak: <span style={{ color: '#34d399', fontWeight: 700 }}>{formatCurrency(maxEq)}</span>
                      </span>
                      <span style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: '0.85rem', color: '#d4d4d8' }}>
                        Trough: <span style={{ color: '#fb7185', fontWeight: 700 }}>{formatCurrency(minEq)}</span>
                      </span>
                    </div>
                    <div style={{ position: 'absolute', bottom: '0.5rem', left: '1.5rem', fontSize: '0.8rem', color: '#71717a', fontFamily: 'JetBrains Mono, monospace' }}>
                      {data[0].date}
                    </div>
                    <div style={{ position: 'absolute', bottom: '0.5rem', right: '1.5rem', fontSize: '0.8rem', color: '#71717a', fontFamily: 'JetBrains Mono, monospace' }}>
                      {data[data.length - 1].date}
                    </div>
                  </>
                );
              })()}
            </div>
          </div>
        )}
      </div>
    );
  }

  // --- LIST VIEW ---
  return (
    <div>
      <div className="page-header">
        <h1>Backtests</h1>
        <button className="btn-primary" onClick={() => setShowRunForm(true)}>
          <Play size={16} /> Run Backtest
        </button>
      </div>

      {error && (
        <div className="card" style={{ borderColor: 'rgba(255,0,85,0.3)', marginBottom: '2rem' }}>
          <p style={{ color: '#fb7185', margin: 0 }}>{error}</p>
        </div>
      )}

      {showRunForm && (
        <div className="card animate-slide-up" style={{ marginBottom: '2rem', borderColor: 'rgba(0,240,255,0.3)' }}>
          <div className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Play size={14} /> Configure Backtest Run
          </div>
          <form onSubmit={handleRunBacktest} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr auto', gap: '1rem', alignItems: 'end', marginTop: '1rem' }}>
            <div>
              <label style={{ color: '#d4d4d8', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', display: 'block' }}>Dataset</label>
              <select value={selectedDataset} onChange={(e) => setSelectedDataset(e.target.value)}
                style={{ width: '100%', padding: '0.8rem', background: 'rgba(0,0,0,0.4)', color: '#fff', border: '1px solid rgba(255,255,255,0.12)', borderRadius: '8px', fontFamily: 'Outfit, sans-serif', fontSize: '0.95rem' }}>
                <option value="">Select dataset...</option>
                {datasets.map(d => <option key={d.id} value={d.id}>{d.name} ({d.symbol} - {d.totalBars} bars)</option>)}
              </select>
            </div>
            <div>
              <label style={{ color: '#d4d4d8', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', display: 'block' }}>Strategy</label>
              <select value={selectedStrategy} onChange={(e) => setSelectedStrategy(e.target.value)}
                style={{ width: '100%', padding: '0.8rem', background: 'rgba(0,0,0,0.4)', color: '#fff', border: '1px solid rgba(255,255,255,0.12)', borderRadius: '8px', fontFamily: 'Outfit, sans-serif', fontSize: '0.95rem' }}>
                <option value="">Select strategy...</option>
                {strategies.map(s => <option key={s.name} value={s.name}>{s.name} ({s.symbol})</option>)}
              </select>
            </div>
            <div>
              <label style={{ color: '#d4d4d8', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', display: 'block' }}>Starting Equity ($)</label>
              <input type="text" value={startingEquity} onChange={(e) => setStartingEquity(e.target.value)} placeholder="100000" style={{ width: '100%', padding: '0.8rem', boxSizing: 'border-box' }} />
            </div>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button type="submit" className="btn-primary" disabled={running || !selectedDataset || !selectedStrategy}>
                {running ? 'Running...' : 'Execute'}
              </button>
              <button type="button" onClick={() => setShowRunForm(false)} style={{ padding: '0.8rem 1rem', background: 'transparent', border: '1px solid rgba(255,255,255,0.12)', borderRadius: '8px', color: '#d4d4d8', cursor: 'pointer', fontFamily: 'Outfit, sans-serif', fontWeight: 600 }}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="table-container animate-slide-up" style={{ animationDelay: '0.3s' }}>
        <div className="table-header-row">
          <h3 style={{ margin: 0, color: '#e4e4e7' }}>Backtest Runs</h3>
        </div>
        <table>
          <thead>
            <tr>
              <th>Strategy</th>
              <th>Status</th>
              <th>Starting $</th>
              <th>Final $</th>
              <th>P&L</th>
              <th>Win Rate</th>
              <th>Profit Factor</th>
              <th>Trades</th>
            </tr>
          </thead>
          <tbody>
            {runs.length === 0 ? (
              <tr><td colSpan={8} style={{ textAlign: 'center', color: '#71717a', padding: '3rem' }}>No backtest runs yet. Upload a dataset and run your first backtest!</td></tr>
            ) : (
              runs.map(r => {
                const pnl = r.totalPnl ? parseFloat(r.totalPnl) : 0;
                return (
                  <tr key={r.id} onClick={() => setSelectedRun(r)} style={{ cursor: 'pointer' }}>
                    <td style={{ fontWeight: 600, color: '#22d3ee' }}>{r.strategyName}</td>
                    <td>{statusBadge(r.status)}</td>
                    <td style={{ fontFamily: 'JetBrains Mono, monospace', color: '#d4d4d8' }}>{formatCurrency(r.startingEquity)}</td>
                    <td style={{ fontFamily: 'JetBrains Mono, monospace', color: '#d4d4d8' }}>{r.finalEquity ? formatCurrency(r.finalEquity) : '—'}</td>
                    <td style={{ fontFamily: 'JetBrains Mono, monospace', fontWeight: 700, color: pnl > 0 ? '#34d399' : pnl < 0 ? '#fb7185' : '#a1a1aa' }}>
                      {r.totalPnl ? `${pnl > 0 ? '+' : ''}${formatCurrency(r.totalPnl)}` : '—'}
                    </td>
                    <td style={{ fontFamily: 'JetBrains Mono, monospace', color: '#d4d4d8' }}>{r.winRate != null ? `${parseFloat(r.winRate).toFixed(1)}%` : '—'}</td>
                    <td style={{ fontFamily: 'JetBrains Mono, monospace', color: '#d4d4d8' }}>{r.profitFactor != null ? parseFloat(r.profitFactor).toFixed(2) : '—'}</td>
                    <td style={{ fontFamily: 'JetBrains Mono, monospace', color: '#d4d4d8' }}>{r.totalTrades || '—'}</td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default Backtests;
