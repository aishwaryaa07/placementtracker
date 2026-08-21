import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, extractErrorMessage } from "../api/client";
import type { Application } from "../api/types";
import { StatusBadge } from "../components/StatusBadge";

export function ApplicationsPage() {
  const [applications, setApplications] = useState<Application[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api
      .get<Application[]>("/api/student/applications")
      .then((response) => {
        if (!cancelled) setApplications(response.data);
      })
      .catch((err) => {
        if (!cancelled) setError(extractErrorMessage(err));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (error) return <p className="error-text">{error}</p>;
  if (applications === null) return <p>Loading applications...</p>;
  if (applications.length === 0) return <p>You haven't applied to any drives yet.</p>;

  return (
    <div>
      <h1>My Applications</h1>
      <div className="card-grid">
        {applications.map((application) => (
          <Link to={`/applications/${application.id}`} key={application.id} className="drive-card">
            <div className="drive-card-header">
              <h2>{application.drive.role}</h2>
              <StatusBadge status={application.status} />
            </div>
            <p className="company-name">{application.drive.company.name}</p>
            <p className="deadline">Applied {new Date(application.appliedAt).toLocaleDateString()}</p>
            {application.offer && (
              <span className="eligible-tag eligible">Offer: {application.offer.status}</span>
            )}
          </Link>
        ))}
      </div>
    </div>
  );
}
