import React from 'react';
import { formatCurrency } from '../../utils/formatters';

const AccountCard = ({ equity }) => (
  <div className="card">
    <h2 className="card-title">Live Account Equity</h2>
    <div className="card-value">
      {formatCurrency(equity)}
    </div>
    <div className="card-subtitle">Fetched from Alpaca Broker API</div>
  </div>
);

export default AccountCard;
