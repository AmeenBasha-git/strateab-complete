import React, { useState } from 'react';
import { parseStrategyDescription, createStrategyFromParsed } from '../api/dashboardService';
import { X, Sparkles, Save, ArrowLeft, Loader2 } from 'lucide-react';

const CreateStrategyModal = ({ isOpen, onClose, onCreated }) => {
  const [step, setStep] = useState(1);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [parsedConfig, setParsedConfig] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleParse = async () => {
    if (!description.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const config = await parseStrategyDescription(description);
      setParsedConfig(config);
      setStep(2);
    } catch (err) {
      setError(err.message || 'Failed to parse strategy description');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    if (!name.trim() || !parsedConfig) return;
    setLoading(true);
    setError(null);
    try {
      await createStrategyFromParsed({
        name,
        originalDescription: description,
        symbol: parsedConfig.symbol,
        entryIndicator: parsedConfig.entryIndicator,
        entryCondition: parsedConfig.entryCondition,
        entryThreshold: parsedConfig.entryThreshold,
        takeProfitPercentage: parsedConfig.takeProfitPercentage,
        stopLossPercentage: parsedConfig.stopLossPercentage,
        timeframe: parsedConfig.timeframe,
      });
      onCreated();
      handleClose();
    } catch (err) {
      setError(err.message || 'Failed to create strategy');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setStep(1);
    setName('');
    setDescription('');
    setParsedConfig(null);
    setError(null);
    setLoading(false);
    onClose();
  };

  const updateConfig = (field, value) => {
    setParsedConfig(prev => ({ ...prev, [field]: value }));
  };

  const inputStyle = {
    width: '100%',
    padding: '10px 12px',
    borderRadius: '8px',
    background: 'rgba(0,0,0,0.3)',
    border: '1px solid var(--card-border)',
    color: 'white',
    outline: 'none',
    fontFamily: 'inherit',
    fontSize: '0.9rem',
    boxSizing: 'border-box',
  };

  const labelStyle = {
    display: 'block',
    marginBottom: '6px',
    fontSize: '0.75rem',
    color: 'var(--text-secondary)',
    fontWeight: 600,
    textTransform: 'uppercase',
    letterSpacing: '0.5px',
  };

  const selectStyle = {
    ...inputStyle,
    appearance: 'none',
    backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' fill='%23888' viewBox='0 0 16 16'%3E%3Cpath d='M8 11L3 6h10z'/%3E%3C/svg%3E")`,
    backgroundRepeat: 'no-repeat',
    backgroundPosition: 'right 12px center',
    paddingRight: '32px',
  };

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      zIndex: 9999,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'rgba(0, 0, 0, 0.75)',
      backdropFilter: 'blur(12px)',
      animation: 'slideUpFade 0.4s ease forwards',
    }}>
      <div style={{
        width: '100%',
        maxWidth: step === 1 ? '520px' : '600px',
        maxHeight: '90vh',
        overflowY: 'auto',
        padding: '32px',
        borderRadius: '16px',
        background: 'var(--card-bg)',
        border: '1px solid var(--card-border)',
        boxShadow: '0 20px 60px rgba(0, 0, 0, 0.6), 0 0 40px rgba(0, 240, 255, 0.1)',
        animation: 'slideUpFade 0.5s cubic-bezier(0.19, 1, 0.22, 1) forwards',
        transition: 'max-width 0.3s ease',
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            {step === 2 && (
              <button onClick={() => setStep(1)} style={{
                background: 'none', border: 'none', color: 'var(--text-secondary)',
                cursor: 'pointer', padding: '4px', display: 'flex',
              }}>
                <ArrowLeft size={18} />
              </button>
            )}
            <h2 style={{ margin: 0, fontSize: '1.3rem', fontWeight: 700, color: '#fff' }}>
              {step === 1 ? 'Describe Your Strategy' : 'Review & Create'}
            </h2>
          </div>
          <button onClick={handleClose} style={{
            background: 'none', border: 'none', color: 'var(--text-muted)',
            cursor: 'pointer', padding: '4px', display: 'flex',
          }}>
            <X size={20} />
          </button>
        </div>

        {error && (
          <div style={{
            background: 'rgba(255, 0, 85, 0.1)',
            border: '1px solid rgba(255, 0, 85, 0.3)',
            color: 'var(--accent-rose)',
            padding: '12px',
            borderRadius: '8px',
            marginBottom: '16px',
            fontSize: '0.85rem',
          }}>
            {error}
          </div>
        )}

        {step === 1 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={labelStyle}>Strategy Name</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="My RSI Strategy"
                style={inputStyle}
              />
            </div>
            <div>
              <label style={labelStyle}>Describe Your Strategy</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Example: Buy SPY when RSI drops below 30. Take profit at 5% gain, stop loss at 2%. Trade on daily timeframe."
                rows={5}
                style={{ ...inputStyle, resize: 'vertical', minHeight: '120px', lineHeight: '1.5' }}
              />
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '6px', display: 'block' }}>
                Describe your entry conditions, exit rules, target symbol, and any indicators you want to use.
              </span>
            </div>
            <button
              onClick={handleParse}
              disabled={loading || !description.trim()}
              className="btn-primary"
              style={{
                width: '100%',
                justifyContent: 'center',
                padding: '12px',
                fontSize: '0.95rem',
                gap: '8px',
                opacity: loading || !description.trim() ? 0.5 : 1,
              }}
            >
              {loading ? <Loader2 size={16} className="spin" /> : <Sparkles size={16} />}
              {loading ? 'Analyzing Strategy...' : 'Parse with AI'}
            </button>
          </div>
        )}

        {step === 2 && parsedConfig && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div style={{
              background: 'rgba(6, 182, 212, 0.08)',
              border: '1px solid rgba(6, 182, 212, 0.2)',
              borderRadius: '8px',
              padding: '12px',
              fontSize: '0.85rem',
              color: 'var(--text-secondary)',
              lineHeight: '1.4',
            }}>
              <strong style={{ color: 'var(--accent-cyan)' }}>AI Parsed:</strong> {parsedConfig.description}
            </div>

            <div>
              <label style={labelStyle}>Strategy Name</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="My Strategy"
                style={inputStyle}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div>
                <label style={labelStyle}>Symbol</label>
                <input
                  type="text"
                  value={parsedConfig.symbol}
                  onChange={(e) => updateConfig('symbol', e.target.value)}
                  style={inputStyle}
                />
              </div>
              <div>
                <label style={labelStyle}>Timeframe</label>
                <select
                  value={parsedConfig.timeframe}
                  onChange={(e) => updateConfig('timeframe', e.target.value)}
                  style={selectStyle}
                >
                  <option value="1m">1 Minute</option>
                  <option value="5m">5 Minutes</option>
                  <option value="15m">15 Minutes</option>
                  <option value="1h">1 Hour</option>
                  <option value="4h">4 Hours</option>
                  <option value="1d">1 Day</option>
                </select>
              </div>
            </div>

            <div style={{
              borderTop: '1px solid var(--card-border)',
              paddingTop: '14px',
              marginTop: '4px',
            }}>
              <span style={{ ...labelStyle, marginBottom: '12px' }}>Entry Conditions</span>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ ...labelStyle, fontSize: '0.7rem' }}>Indicator</label>
                  <select
                    value={parsedConfig.entryIndicator}
                    onChange={(e) => updateConfig('entryIndicator', e.target.value)}
                    style={selectStyle}
                  >
                    <option value="RSI">RSI</option>
                    <option value="MACD">MACD</option>
                    <option value="SMA">SMA</option>
                    <option value="EMA">EMA</option>
                    <option value="VWAP">VWAP</option>
                    <option value="PRICE">Price</option>
                    <option value="BOLLINGER">Bollinger</option>
                  </select>
                </div>
                <div>
                  <label style={{ ...labelStyle, fontSize: '0.7rem' }}>Condition</label>
                  <select
                    value={parsedConfig.entryCondition}
                    onChange={(e) => updateConfig('entryCondition', e.target.value)}
                    style={selectStyle}
                  >
                    <option value="<">Below (&lt;)</option>
                    <option value=">">Above (&gt;)</option>
                    <option value="==">Equals (==)</option>
                  </select>
                </div>
                <div>
                  <label style={{ ...labelStyle, fontSize: '0.7rem' }}>Threshold</label>
                  <input
                    type="number"
                    value={parsedConfig.entryThreshold}
                    onChange={(e) => updateConfig('entryThreshold', parseFloat(e.target.value) || 0)}
                    style={inputStyle}
                  />
                </div>
              </div>
            </div>

            <div style={{
              borderTop: '1px solid var(--card-border)',
              paddingTop: '14px',
              marginTop: '4px',
            }}>
              <span style={{ ...labelStyle, marginBottom: '12px' }}>Exit Rules</span>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ ...labelStyle, fontSize: '0.7rem' }}>Take Profit %</label>
                  <input
                    type="number"
                    step="0.01"
                    value={(parsedConfig.takeProfitPercentage * 100).toFixed(1)}
                    onChange={(e) => updateConfig('takeProfitPercentage', (parseFloat(e.target.value) || 0) / 100)}
                    style={inputStyle}
                  />
                </div>
                <div>
                  <label style={{ ...labelStyle, fontSize: '0.7rem' }}>Stop Loss %</label>
                  <input
                    type="number"
                    step="0.01"
                    value={(parsedConfig.stopLossPercentage * 100).toFixed(1)}
                    onChange={(e) => updateConfig('stopLossPercentage', (parseFloat(e.target.value) || 0) / 100)}
                    style={inputStyle}
                  />
                </div>
              </div>
            </div>

            <button
              onClick={handleCreate}
              disabled={loading || !name.trim()}
              className="btn-primary"
              style={{
                width: '100%',
                justifyContent: 'center',
                padding: '12px',
                fontSize: '0.95rem',
                gap: '8px',
                marginTop: '8px',
                opacity: loading || !name.trim() ? 0.5 : 1,
              }}
            >
              {loading ? <Loader2 size={16} className="spin" /> : <Save size={16} />}
              {loading ? 'Creating Strategy...' : 'Create Strategy'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default CreateStrategyModal;
