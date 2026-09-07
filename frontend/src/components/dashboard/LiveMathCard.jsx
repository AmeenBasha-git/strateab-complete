import React from 'react';
import { formatCurrency } from '../../utils/formatters';

const LiveMathCard = ({ data }) => (
  <div className="card animate-slide-up" style={{gridColumn: '1 / -1', animationDelay: '0.2s'}}>
    <h2 className="card-title">Live Algorithmic Math (VWAP Bands)</h2>
    
    <div className="math-row">
      <span className="math-label">Current SPY Price</span>
      <span className="math-value data-pulse" style={{color: 'var(--text-primary)', fontSize: '1.2rem'}}>
        {formatCurrency(data.currentPrice)}
      </span>
    </div>

    <div className="math-row">
      <span className="math-label">Upper Volatility Band (+0.5%)</span>
      <span className="math-value data-pulse" style={{color: 'var(--accent-red)'}}>
        {formatCurrency(data.upperBand)}
      </span>
    </div>

    <div className="math-row">
      <span className="math-label">Intraday VWAP (Fair Value)</span>
      <span className="math-value data-pulse" style={{color: 'var(--accent-blue)'}}>
        {formatCurrency(data.vwap)}
      </span>
    </div>

    <div className="math-row">
      <span className="math-label">Lower Volatility Band (-0.5%)</span>
      <span className="math-value data-pulse" style={{color: 'var(--accent-green)'}}>
        {formatCurrency(data.lowerBand)}
      </span>
    </div>
    
  </div>
);

export default LiveMathCard;
