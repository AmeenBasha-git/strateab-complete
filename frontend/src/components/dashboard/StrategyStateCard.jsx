import React from 'react';

const StrategyStateCard = ({ isHalted, symbol, positionState }) => (
  <div className="card">
    <h2 className="card-title">Strategy Engine State</h2>
    <div style={{display: 'flex', flexDirection: 'column', gap: '1rem'}}>
      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
        <span className="math-label">Engine Status</span>
        <span className={`status-badge ${isHalted ? 'halted' : 'running'}`}>
          {isHalted ? 'HALTED (DRAWDOWN)' : 'RUNNING'}
        </span>
      </div>
      
      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
        <span className="math-label">Target Symbol</span>
        <span className="math-value">{symbol}</span>
      </div>

      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
        <span className="math-label">Current Position</span>
        <span className={`status-badge ${positionState === 'LONG' ? 'long' : positionState === 'SHORT' ? 'short' : ''}`}>
          {positionState}
        </span>
      </div>
    </div>
  </div>
);

export default StrategyStateCard;
