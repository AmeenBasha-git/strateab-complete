const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const fetchDashboardStatus = async () => {
  const response = await fetch(`${BASE_URL}/dashboard/status`);
  if (!response.ok) {
    throw new Error('Failed to fetch data from engine');
  }
  return await response.json();
};

export const fetchStrategyDetails = async (strategyId) => {
  const response = await fetch(`${BASE_URL}/strategies/${strategyId}`);
  if (!response.ok) {
    throw new Error('Failed to fetch strategy details');
  }
  return await response.json();
};

export const fetchTrades = async () => {
  const response = await fetch(`${BASE_URL}/dashboard/trades`);
  if (!response.ok) {
    throw new Error('Failed to fetch trades');
  }
  return await response.json();
};

export const fetchLibraryStrategies = async () => {
  const response = await fetch(`${BASE_URL}/strategies/library`);
  if (!response.ok) {
    throw new Error('Failed to fetch strategy library');
  }
  return await response.json();
};

// ============ Backtest API ============

export const importDatasetFromUrl = async (name, symbol, timeframe, sourceType, sourceUri, { exchange, fromDate, toDate } = {}) => {
  const response = await fetch(`${BASE_URL}/v1/backtests/datasets/import-url`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, symbol, timeframe, sourceType, sourceUri, exchange, fromDate, toDate }),
  });
  if (!response.ok) {
    throw new Error('Failed to import dataset');
  }
  return await response.json();
};

export const fetchDatasets = async () => {
  const response = await fetch(`${BASE_URL}/v1/backtests/datasets`);
  if (!response.ok) {
    throw new Error('Failed to fetch datasets');
  }
  return await response.json();
};

export const runBacktest = async (datasetId, strategyName, startingEquity) => {
  const response = await fetch(`${BASE_URL}/v1/backtests/run`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ datasetId, strategyName, startingEquity }),
  });
  if (!response.ok) {
    throw new Error('Failed to run backtest');
  }
  return await response.json();
};

export const fetchBacktestRuns = async () => {
  const response = await fetch(`${BASE_URL}/v1/backtests/runs`);
  if (!response.ok) {
    throw new Error('Failed to fetch backtest runs');
  }
  return await response.json();
};

export const fetchBacktestRunCount = async () => {
  const response = await fetch(`${BASE_URL}/v1/backtests/runs/count`);
  if (!response.ok) {
    throw new Error('Failed to fetch backtest count');
  }
  return await response.json();
};

export const fetchBacktestStrategies = async () => {
  const response = await fetch(`${BASE_URL}/v1/backtests/strategies`);
  if (!response.ok) {
    throw new Error('Failed to fetch backtest strategies');
  }
  return await response.json();
};

