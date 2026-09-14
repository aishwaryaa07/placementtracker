import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import InterviewerSidebar from './InterviewerSidebar';
import InterviewerNavbar from './InterviewerNavbar';

export default function InterviewerLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="admin-layout interviewer-app">
      <InterviewerSidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="admin-main">
        <InterviewerNavbar onMenuClick={() => setSidebarOpen(true)} />
        <main className="admin-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
