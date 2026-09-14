import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import { useAuth } from '../context/useAuth';
import AdminLayout from '../components/layout/AdminLayout';
import StudentLayout from '../components/layout/StudentLayout';
import InterviewerLayout from '../components/layout/InterviewerLayout';
import Login from '../pages/Login';
import Dashboard from '../pages/Dashboard';
import Companies from '../pages/Companies';
import Interviewers from '../pages/Interviewers';
import Drives from '../pages/Drives';
import DriveDetails from '../pages/DriveDetails';
import RoundScoreImport from '../pages/RoundScoreImport';
import PanelAssignment from '../pages/PanelAssignment';
import Applications from '../pages/Applications';
import ApplicationDetails from '../pages/ApplicationDetails';
import CandidateProfiles from '../pages/CandidateProfiles';
import OfferTracker from '../pages/OfferTracker';
import Reports from '../pages/Reports';
import StudentLogin from '../pages/student/StudentLogin';
import StudentRegister from '../pages/student/StudentRegister';
import StudentDashboard from '../pages/student/StudentDashboard';
import StudentProfile from '../pages/student/StudentProfile';
import StudentDrives from '../pages/student/StudentDrives';
import StudentDriveDetails from '../pages/student/StudentDriveDetails';
import StudentApplications from '../pages/student/StudentApplications';
import StudentApplicationDetails from '../pages/student/StudentApplicationDetails';
import StudentOffers from '../pages/student/StudentOffers';
import InterviewerLogin from '../pages/interviewer/InterviewerLogin';
import InterviewerDashboard from '../pages/interviewer/InterviewerDashboard';
import InterviewerDrives from '../pages/interviewer/InterviewerDrives';

function homePathFor(role) {
  if (role === 'STUDENT') return '/student/dashboard';
  if (role === 'INTERVIEWER') return '/interviewer/dashboard';
  return '/dashboard';
}

function HomeRedirect() {
  const { isAuthenticated, role } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <Navigate to={homePathFor(role)} replace />;
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/student/login" element={<StudentLogin />} />
      <Route path="/register" element={<StudentRegister />} />
      <Route path="/interviewer/login" element={<InterviewerLogin />} />

      <Route element={<ProtectedRoute role="ADMIN" />}>
        <Route element={<AdminLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/companies" element={<Companies />} />
          <Route path="/interviewers" element={<Interviewers />} />
          <Route path="/drives" element={<Drives />} />
          <Route path="/drives/:id" element={<DriveDetails />} />
          <Route path="/rounds/:roundId/score-import" element={<RoundScoreImport />} />
          <Route path="/rounds/:roundId/panel-assignments" element={<PanelAssignment />} />
          <Route path="/applications" element={<Applications />} />
          <Route path="/applications/:id" element={<ApplicationDetails />} />
          <Route path="/candidates" element={<CandidateProfiles />} />
          <Route path="/offers" element={<OfferTracker />} />
          <Route path="/reports" element={<Reports />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute role="INTERVIEWER" />}>
        <Route element={<InterviewerLayout />}>
          <Route path="/interviewer/dashboard" element={<InterviewerDashboard />} />
          <Route path="/interviewer/drives" element={<InterviewerDrives />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute role="STUDENT" />}>
        <Route element={<StudentLayout />}>
          <Route path="/student/dashboard" element={<StudentDashboard />} />
          <Route path="/student/profile" element={<StudentProfile />} />
          <Route path="/student/drives" element={<StudentDrives />} />
          <Route path="/student/drives/:id" element={<StudentDriveDetails />} />
          <Route path="/student/applications" element={<StudentApplications />} />
          <Route path="/student/applications/:id" element={<StudentApplicationDetails />} />
          <Route path="/student/offers" element={<StudentOffers />} />
        </Route>
      </Route>

      <Route path="/" element={<HomeRedirect />} />
      <Route path="*" element={<HomeRedirect />} />
    </Routes>
  );
}
