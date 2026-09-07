import React from 'react';

const PlaceholderPage = ({ title }) => (
  <div>
    <div className="page-header">
      <h1>{title}</h1>
    </div>
    <div className="card" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '400px' }}>
      <p style={{ color: 'var(--text-muted)' }}>This section is currently under construction.</p>
    </div>
  </div>
);

export default PlaceholderPage;
