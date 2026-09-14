import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8081',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // 401 = no valid session (missing/expired/invalid token) -> log out.
    // 403 = authenticated but not permitted (e.g. non-admin role) -> let the
    // calling page show its own error; the session itself is still valid.
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('email');
      localStorage.removeItem('name');
      localStorage.removeItem('role');
      const onStudentSide = window.location.pathname.startsWith('/student');
      const onInterviewerSide = window.location.pathname.startsWith('/interviewer');
      const loginPath = onStudentSide ? '/student/login' : onInterviewerSide ? '/interviewer/login' : '/login';
      if (window.location.pathname !== loginPath) {
        window.location.href = loginPath;
      }
    }
    return Promise.reject(error);
  }
);

export default api;
