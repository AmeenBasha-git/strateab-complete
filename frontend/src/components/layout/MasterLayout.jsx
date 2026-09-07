import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopNav from './TopNav';

const MasterLayout = () => {
  return (
    <div className="app-container">
      <Sidebar />
      <div className="main-content">
        <TopNav />
        <div className="page-content">
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default MasterLayout;
