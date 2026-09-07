import React from 'react';

const KpiCard = ({ title, value, subtitle }) => (
  <div className="card">
    <h2 className="card-title">{title}</h2>
    <div className="card-value">
      {value}
    </div>
    {subtitle && <div className="card-subtitle">{subtitle}</div>}
  </div>
);

export default KpiCard;
