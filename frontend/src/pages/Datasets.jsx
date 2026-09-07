import React, { useState, useEffect, useMemo, useRef } from 'react';
import { Upload, Database, CheckCircle, XCircle, Calendar, BarChart2, Clock, Trash2, Globe, Search } from 'lucide-react';
import { importDatasetFromUrl, fetchDatasets } from '../api/dashboardService';
import usStocksRaw from '../data/us_stocks.json';
import usEtfsRaw from '../data/us_etfs.json';

const ALL_SYMBOLS = [
  ...usStocksRaw.map(s => ({ symbol: s.s, name: s.n, type: 'Stock', sector: s.sec || '' })),
  ...usEtfsRaw.map(e => ({ symbol: e.s, name: e.n, type: 'ETF', sector: '' })),
];

const SymbolSearch = ({ value, onChange, placeholder }) => {
  const [query, setQuery] = useState(value || '');
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => { setQuery(value || ''); }, [value]);

  useEffect(() => {
    const handler = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const results = useMemo(() => {
    if (!query || query.length < 1) return [];
    const q = query.toUpperCase();
    const exact = [];
    const startsWith = [];
    const contains = [];
    for (const item of ALL_SYMBOLS) {
      if (item.symbol === q) { exact.push(item); continue; }
      if (item.symbol.startsWith(q)) { startsWith.push(item); continue; }
      if (item.name.toUpperCase().includes(q) || item.symbol.includes(q)) contains.push(item);
    }
    return [...exact, ...startsWith, ...contains].slice(0, 50);
  }, [query]);

  return (
    <div ref={ref} style={{ position: 'relative' }}>
      <div style={{ position: 'relative' }}>
        <Search size={14} style={{ position: 'absolute', left: '0.8rem', top: '50%', transform: 'translateY(-50%)', color: '#71717a', pointerEvents: 'none' }} />
        <input
          type="text"
          value={query}
          onChange={(e) => { setQuery(e.target.value); onChange(e.target.value.toUpperCase()); setOpen(true); }}
          onFocus={() => setOpen(true)}
          placeholder={placeholder || 'Search AAPL, TSLA, SPY...'}
          style={{ width: '100%', padding: '0.8rem 0.8rem 0.8rem 2.2rem', boxSizing: 'border-box' }}
        />
      </div>
      {open && results.length > 0 && (
        <div style={{
          position: 'absolute', top: '100%', left: 0, right: 0, zIndex: 100,
          background: 'rgba(10,10,18,0.98)', border: '1px solid rgba(0,240,255,0.2)',
          borderRadius: '8px', marginTop: '4px', maxHeight: '280px', overflowY: 'auto',
          boxShadow: '0 12px 40px rgba(0,0,0,0.6)',
          backdropFilter: 'blur(12px)',
        }}>
          {results.map((item) => (
            <div
              key={item.symbol + item.type}
              onClick={() => { onChange(item.symbol); setQuery(item.symbol); setOpen(false); }}
              style={{
                padding: '0.6rem 1rem', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.75rem',
                borderBottom: '1px solid rgba(255,255,255,0.04)',
                transition: 'background 0.15s',
              }}
              onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(0,240,255,0.08)'}
              onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
            >
              <span style={{
                fontFamily: 'JetBrains Mono, monospace', fontWeight: 700, fontSize: '0.9rem',
                color: '#22d3ee', minWidth: '60px',
              }}>{item.symbol}</span>
              <span style={{
                fontSize: '0.78rem', fontWeight: 600, padding: '0.15rem 0.4rem', borderRadius: '4px',
                background: item.type === 'ETF' ? 'rgba(251,191,36,0.15)' : 'rgba(52,211,153,0.15)',
                color: item.type === 'ETF' ? '#fbbf24' : '#34d399',
                border: `1px solid ${item.type === 'ETF' ? 'rgba(251,191,36,0.3)' : 'rgba(52,211,153,0.3)'}`,
              }}>{item.type}</span>
              <span style={{ color: '#a1a1aa', fontSize: '0.82rem', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {item.name}
              </span>
              {item.sector && (
                <span style={{ color: '#52525b', fontSize: '0.72rem', whiteSpace: 'nowrap' }}>{item.sector}</span>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

const Datasets = () => {
  const [datasets, setDatasets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showImport, setShowImport] = useState(false);

  // Import form state
  const [sourceType, setSourceType] = useState('KAGGLE');
  const [downloadUrl, setDownloadUrl] = useState('');
  const [uploadName, setUploadName] = useState('');
  const [uploadSymbol, setUploadSymbol] = useState('');
  const [uploadTimeframe, setUploadTimeframe] = useState('1Day');
  const [uploading, setUploading] = useState(false);
  const [uploadMessage, setUploadMessage] = useState(null);

  // EODHD-specific state
  const [eodhdExchange, setEodhdExchange] = useState('US');
  const [eodhdFromDate, setEodhdFromDate] = useState('');
  const [eodhdToDate, setEodhdToDate] = useState('');

  useEffect(() => {
    loadDatasets();
  }, []);

  const loadDatasets = async () => {
    try {
      setLoading(true);
      const res = await fetchDatasets();
      setDatasets(res.data || []);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleImport = async (e) => {
    e.preventDefault();
    const isEodhd = sourceType === 'EODHD';
    if (isEodhd) {
      if (!uploadSymbol || !eodhdFromDate || !eodhdToDate) return;
    } else {
      if (!downloadUrl || !uploadName || !uploadSymbol) return;
    }

    setUploading(true);
    setUploadMessage(null);
    try {
      const name = isEodhd && !uploadName
        ? `${uploadSymbol.toUpperCase()} ${uploadTimeframe} (EODHD)`
        : uploadName;
      const extras = isEodhd
        ? { exchange: eodhdExchange, fromDate: eodhdFromDate, toDate: eodhdToDate }
        : {};
      const res = await importDatasetFromUrl(name, uploadSymbol, uploadTimeframe, sourceType, downloadUrl, extras);
      if (res.success) {
        setUploadMessage({ type: 'success', text: `Imported: ${res.data.totalBars} bars (${res.data.startDate} → ${res.data.endDate})` });
        setDownloadUrl('');
        setUploadName('');
        setUploadSymbol('');
        setEodhdFromDate('');
        setEodhdToDate('');
        setShowImport(false);
        loadDatasets();
      } else {
        setUploadMessage({ type: 'error', text: res.message || 'Validation failed' });
      }
    } catch (err) {
      setUploadMessage({ type: 'error', text: err.message });
    } finally {
      setUploading(false);
    }
  };

  const formatNumber = (n) => {
    if (n == null) return '—';
    return new Intl.NumberFormat('en-US').format(n);
  };

  if (loading) {
    return <div className="loading-state">Loading datasets...</div>;
  }

  return (
    <div>
      <div className="page-header">
        <h1>Datasets</h1>
        <button className="btn-primary" onClick={() => setShowImport(!showImport)}>
          <Upload size={16} /> Import Dataset
        </button>
      </div>

      {error && (
        <div className="card" style={{ borderColor: 'rgba(255,0,85,0.3)', marginBottom: '2rem' }}>
          <p style={{ color: 'var(--accent-rose)', margin: 0 }}>{error}</p>
        </div>
      )}

      {uploadMessage && !showImport && (
        <div className="card animate-slide-up" style={{
          borderColor: uploadMessage.type === 'success' ? 'rgba(16,185,129,0.3)' : 'rgba(255,0,85,0.3)',
          marginBottom: '2rem'
        }}>
          <p style={{ margin: 0, color: uploadMessage.type === 'success' ? 'var(--accent-emerald)' : 'var(--accent-rose)', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            {uploadMessage.type === 'success' ? <CheckCircle size={16} /> : <XCircle size={16} />}
            {uploadMessage.text}
          </p>
        </div>
      )}

      {/* Import Form */}
      {showImport && (
        <div className="card animate-slide-up" style={{ marginBottom: '2rem', borderColor: 'rgba(0,240,255,0.3)' }}>
          <div className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.5rem' }}>
            {sourceType === 'EODHD' ? <Globe size={14} /> : <Upload size={14} />}
            {sourceType === 'EODHD' ? 'Fetch from EODHD API' : 'Import Dataset from URL'}
          </div>
          <form onSubmit={handleImport}>
            {/* Source type selector */}
            <div style={{ display: 'grid', gridTemplateColumns: sourceType === 'EODHD' ? '1fr' : '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
              <div>
                <label style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', display: 'block' }}>Source Type</label>
                <select
                  value={sourceType}
                  onChange={(e) => { setSourceType(e.target.value); setDownloadUrl(''); }}
                  style={{ width: '100%', padding: '0.8rem', background: 'rgba(0,0,0,0.3)', color: '#fff', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', fontFamily: 'Outfit, sans-serif', fontSize: '0.95rem' }}
                >
                  <option value="KAGGLE">Kaggle Dataset</option>
                  <option value="DIRECT_URL">Direct URL</option>
                  <option value="EODHD">EODHD Intraday API</option>
                </select>
              </div>
              {sourceType !== 'EODHD' && (
                <div>
                  <label style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', display: 'block' }}>
                    {sourceType === 'KAGGLE' ? 'Kaggle Dataset ID' : 'Download URL'}
                  </label>
                  <input
                    type={sourceType === 'KAGGLE' ? 'text' : 'url'}
                    placeholder={sourceType === 'KAGGLE' ? 'tgtanalytics/nq-futures-1min-bar-2022-2025' : 'https://example.com/dataset.csv'}
                    value={downloadUrl}
                    onChange={(e) => setDownloadUrl(e.target.value)}
                    style={{ width: '100%', padding: '0.8rem', boxSizing: 'border-box' }}
                  />
                </div>
              )}
            </div>

            {/* EODHD-specific fields */}
            {sourceType === 'EODHD' && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                <div>
                  <label style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', display: 'block' }}>Symbol</label>
                  <input type="text" value={uploadSymbol} onChange={(e) => setUploadSymbol(e.target.value)} placeholder="AAPL" style={{ width: '100%', padding: '0.8rem', boxSizing: 'border-box' }} />
                </div>
                <div>
                  <label style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', display: 'block' }}>Exchange</label>
                  <select
                    value={eodhdExchange}
                    onChange={(e) => setEodhdExchange(e.target.value)}
                    style={{ width: '100%', padding: '0.8rem', background: 'rgba(0,0,0,0.3)', color: '#fff', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', fontFamily: 'Outfit, sans-serif', fontSize: '0.95rem' }}
                  >
                    <option value="US">US (NYSE/NASDAQ)</option>
                    <option value="LSE">LSE (London)</option>
                    <option value="TO">TSX (Toronto)</option>
                    <option value="AS">Euronext Amsterdam</option>
                    <option value="PA">Euronext Paris</option>
                    <option value="XETRA">XETRA (Frankfurt)</option>
                    <option value="HK">HKEX (Hong Kong)</option>
                    <option value="AU">ASX (Australia)</option>
                    <option value="NSE">NSE (India)</option>
                    <option value="CC">Crypto</option>
                  </select>
                </div>
                <div>
                  <label style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', display: 'block' }}>From Date</label>
                  <input type="date" value={eodhdFromDate} onChange={(e) => setEodhdFromDate(e.target.value)}
                    style={{ width: '100%', padding: '0.8rem', background: 'rgba(0,0,0,0.3)', color: '#fff', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', fontFamily: 'Outfit, sans-serif', fontSize: '0.95rem', boxSizing: 'border-box' }} />
                </div>
                <div>
                  <label style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', display: 'block' }}>To Date</label>
                  <input type="date" value={eodhdToDate} onChange={(e) => setEodhdToDate(e.target.value)}
                    style={{ width: '100%', padding: '0.8rem', background: 'rgba(0,0,0,0.3)', color: '#fff', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', fontFamily: 'Outfit, sans-serif', fontSize: '0.95rem', boxSizing: 'border-box' }} />
                </div>
              </div>
            )}

            {/* Common fields */}
            <div style={{ display: 'grid', gridTemplateColumns: sourceType === 'EODHD' ? '2fr 1fr' : '2fr 1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
              <div>
                <label style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', display: 'block' }}>Dataset Name {sourceType === 'EODHD' && <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}>(auto-generated if empty)</span>}</label>
                <input type="text" value={uploadName} onChange={(e) => setUploadName(e.target.value)} placeholder={sourceType === 'EODHD' ? 'AAPL 5Min (EODHD)' : 'SPY 5min Intraday OHLC'} style={{ width: '100%', padding: '0.8rem', boxSizing: 'border-box' }} />
              </div>
              {sourceType !== 'EODHD' && (
                <div>
                  <label style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', display: 'block' }}>Symbol</label>
                  <input type="text" value={uploadSymbol} onChange={(e) => setUploadSymbol(e.target.value)} placeholder="SPY" style={{ width: '100%', padding: '0.8rem', boxSizing: 'border-box' }} />
                </div>
              )}
              <div>
                <label style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem', display: 'block' }}>Timeframe</label>
                <select
                  value={uploadTimeframe}
                  onChange={(e) => setUploadTimeframe(e.target.value)}
                  style={{ width: '100%', padding: '0.8rem', background: 'rgba(0,0,0,0.3)', color: '#fff', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', fontFamily: 'Outfit, sans-serif', fontSize: '0.95rem' }}
                >
                  {sourceType === 'EODHD' ? (
                    <>
                      <option value="1Min">1 Minute</option>
                      <option value="5Min">5 Minutes</option>
                      <option value="15Min">15 Minutes</option>
                      <option value="1Hour">1 Hour</option>
                    </>
                  ) : (
                    <>
                      <option value="1Min">1 Minute</option>
                      <option value="5Min">5 Minutes</option>
                      <option value="15Min">15 Minutes</option>
                      <option value="1Hour">1 Hour</option>
                      <option value="1Day">1 Day</option>
                    </>
                  )}
                </select>
              </div>
            </div>
            {uploadMessage && (
              <p style={{ marginBottom: '1rem', color: uploadMessage.type === 'success' ? 'var(--accent-emerald)' : 'var(--accent-rose)', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                {uploadMessage.type === 'success' ? <CheckCircle size={14} /> : <XCircle size={14} />}
                {uploadMessage.text}
              </p>
            )}
            <div style={{ display: 'flex', gap: '0.75rem' }}>
              <button type="submit" className="btn-primary" disabled={
                uploading ||
                !uploadSymbol ||
                (sourceType === 'EODHD' ? (!eodhdFromDate || !eodhdToDate) : (!downloadUrl || !uploadName))
              }>
                {uploading ? 'Importing...' : sourceType === 'EODHD' ? 'Fetch & Import' : 'Import Dataset'}
              </button>
              <button type="button" onClick={() => { setShowImport(false); setUploadMessage(null); }} style={{ padding: '0.8rem 1.2rem', background: 'transparent', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', color: 'var(--text-secondary)', cursor: 'pointer', fontFamily: 'Outfit, sans-serif', fontWeight: 600 }}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Summary Cards */}
      <div className="grid-container animate-slide-up" style={{ gridTemplateColumns: 'repeat(3, 1fr)', marginBottom: '2rem', animationDelay: '0.1s' }}>
        <div className="card">
          <div className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}><Database size={14} /> Total Datasets</div>
          <div className="card-value">{datasets.length}</div>
          <div className="card-subtitle">Imported & ready for backtesting</div>
        </div>
        <div className="card">
          <div className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}><BarChart2 size={14} /> Total Bars</div>
          <div className="card-value">{formatNumber(datasets.reduce((sum, d) => sum + (d.totalBars || 0), 0))}</div>
          <div className="card-subtitle">Across all datasets</div>
        </div>
        <div className="card">
          <div className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}><Calendar size={14} /> Symbols</div>
          <div className="card-value">{[...new Set(datasets.map(d => d.symbol))].length}</div>
          <div className="card-subtitle">{[...new Set(datasets.map(d => d.symbol))].join(', ') || 'None'}</div>
        </div>
      </div>

      {/* Datasets Table */}
      <div className="table-container animate-slide-up" style={{ animationDelay: '0.2s' }}>
        <div className="table-header-row">
          <h3 style={{ margin: 0 }}>All Datasets</h3>
        </div>
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Symbol</th>
              <th>Timeframe</th>
              <th>Start Date</th>
              <th>End Date</th>
              <th>Total Bars</th>
              <th>Uploaded</th>
            </tr>
          </thead>
          <tbody>
            {datasets.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '3rem' }}>
                  No datasets imported yet. Click "Import Dataset" to get started.
                </td>
              </tr>
            ) : (
              datasets.map(d => (
                <tr key={d.id}>
                  <td style={{ fontWeight: 600, color: 'var(--accent-cyan)' }}>{d.name}</td>
                  <td>
                    <span style={{
                      fontFamily: 'JetBrains Mono, monospace',
                      fontWeight: 700,
                      background: 'rgba(0,240,255,0.08)',
                      padding: '0.25rem 0.6rem',
                      borderRadius: '4px',
                      border: '1px solid rgba(0,240,255,0.15)',
                      fontSize: '0.9rem'
                    }}>
                      {d.symbol}
                    </span>
                  </td>
                  <td>
                    <span style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.3rem',
                      fontFamily: 'JetBrains Mono, monospace',
                      fontSize: '0.9rem',
                      background: 'rgba(255,255,255,0.04)',
                      padding: '0.25rem 0.6rem',
                      borderRadius: '4px'
                    }}>
                      <Clock size={12} style={{ color: 'var(--text-muted)' }} />
                      {d.timeframe}
                    </span>
                  </td>
                  <td style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: '0.9rem' }}>{d.startDate}</td>
                  <td style={{ fontFamily: 'JetBrains Mono, monospace', fontSize: '0.9rem' }}>{d.endDate}</td>
                  <td style={{ fontFamily: 'JetBrains Mono, monospace', fontWeight: 600 }}>{formatNumber(d.totalBars)}</td>
                  <td style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                    {d.createdAt ? new Date(d.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : '—'}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default Datasets;
