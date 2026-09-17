import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import MasterLayout from './components/layout/MasterLayout';
import Dashboard from './pages/Dashboard';
import Strategies from './pages/Strategies';
import StrategyDetails from './pages/StrategyDetails';
import LiveMonitoring from './pages/LiveMonitoring';
import PlaceholderPage from './pages/PlaceholderPage';
import Backtests from './pages/Backtests';
import Datasets from './pages/Datasets';
import AuthPage from './pages/AuthPage';
import './index.css';

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<AuthPage />} />
          <Route path="/" element={
            <ProtectedRoute>
              <MasterLayout />
            </ProtectedRoute>
          }>
            <Route index element={<Dashboard />} />
            <Route path="strategies" element={<Strategies />} />
            <Route path="strategies/:id" element={<StrategyDetails />} />
            <Route path="datasets" element={<Datasets />} />
            <Route path="backtests" element={<Backtests />} />
            <Route path="deployments" element={<PlaceholderPage title="Deployments" />} />
            <Route path="live" element={<LiveMonitoring />} />
            <Route path="metrics" element={<PlaceholderPage title="Metrics & Analytics" />} />
            <Route path="reports" element={<PlaceholderPage title="Reports" />} />
            <Route path="notifications" element={<PlaceholderPage title="Notifications" />} />
            <Route path="admin" element={<PlaceholderPage title="Administration" />} />
            <Route path="settings" element={<PlaceholderPage title="Settings" />} />
            <Route path="profile" element={<PlaceholderPage title="User Profile" />} />
          </Route>
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;
