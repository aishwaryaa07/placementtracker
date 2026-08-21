import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, extractErrorMessage } from "../api/client";
import type { StudentDriveView } from "../api/types";
import { StatusBadge } from "../components/StatusBadge";

export function DrivesPage() {
  const [drives, setDrives] = useState<StudentDriveView[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api
      .get<StudentDriveView[]>("/api/student/drives")
      .then((response) => {
        if (!cancelled) setDrives(response.data);
      })
      .catch((err) => {
        if (!cancelled) setError(extractErrorMessage(err));
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (error) return <p className="error-text">{error}</p>;
  if (drives === null) return <p>Loading drives...</p>;
  if (drives.length === 0) return <p>No open drives right now. Check back later.</p>;

  return (
    <div>
      <h1>Open Drives</h1>
      <div className="card-grid">
        {drives.map(({ drive, eligible }) => (
          <Link to={`/drives/${drive.id}`} key={drive.id} className="drive-card">
            <div className="drive-card-header">
              <h2>{drive.role}</h2>
              <StatusBadge status={drive.status} />
            </div>
            <p className="company-name">{drive.company.name}</p>
            {drive.ctc != null && <p className="ctc">CTC: ₹{drive.ctc.toLocaleString()}</p>}
            {drive.applicationDeadline && <p className="deadline">Apply by {drive.applicationDeadline}</p>}
            <span className={eligible ? "eligible-tag eligible" : "eligible-tag not-eligible"}>
              {eligible ? "You're eligible" : "Not eligible"}
            </span>
          </Link>
        ))}
      </div>
    </div>
  );
}
