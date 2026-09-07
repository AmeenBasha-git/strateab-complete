import React from 'react';

const ErrorState = ({ error }) => (
  <div className="loading-state">
    <h2 style={{color: 'var(--accent-red)'}}>Connection Lost</h2>
    <p>Failed to connect to the backend server. Please check the network.</p>
    <p style={{opacity: 0.5}}>{error}</p>
  </div>
);

export default ErrorState;
