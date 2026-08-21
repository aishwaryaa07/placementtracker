import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { api, extractErrorMessage } from "../api/client";
import type { Application, StudentDriveView } from "../api/types";
import { StatusBadge } from "../components/StatusBadge";

export function DriveDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [view, setView] = useState<StudentDriveView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [applyError, setApplyError] = useState<string | null>(null);
  const [applying, setApplying] = useState(false);

  useEffect(() => {
    let cancelled = false;
    api
      .get<StudentDriveView>(`/api/student/drives/${id}`)
      .then((response) => {
        if (!cancelled) setView(response.data);
      })
      .catch((err) => {
        if (!cancelled) setError(extractErrorMessage(err));
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  const handleApply = async () => {
    setApplying(true);
    setApplyError(null);
    try {
      const response = await api.post<Application>(`/api/student/drives/${id}/apply`);
      navigate(`/applications/${response.data.id}`);
    } catch (err) {
      setApplyError(extractErrorMessage(err));
    } finally {
      setApplying(false);
    }
  };

  if (error) return <p className="error-text">{error}</p>;
  if (view === null) return <p>Loading drive...</p>;

  const { drive, eligible } = view;
  const canApply = eligible && drive.status !== "CLOSED";

  return (
    <div className="page-card">
      <Link to="/drives" className="back-link">
        &larr; Back to drives
      </Link>
      <div className="drive-card-header">
        <h1>{drive.role}</h1>
        <StatusBadge status={drive.status} />
      </div>
      <p className="company-name">{drive.company.name}</p>
      {drive.description && <p>{drive.description}</p>}

      <dl className="detail-list">
        {drive.ctc != null && (
          <>
            <dt>CTC</dt>
            <dd>₹{drive.ctc.toLocaleString()}</dd>
          </>
        )}
        {drive.minCgpa != null && (
          <>
            <dt>Minimum CGPA</dt>
            <dd>{drive.minCgpa}</dd>
          </>
        )}
        {drive.eligibleBranches.length > 0 && (
          <>
            <dt>Eligible branches</dt>
            <dd>{drive.eligibleBranches.join(", ")}</dd>
          </>
        )}
        {drive.applicationDeadline && (
          <>
            <dt>Application deadline</dt>
            <dd>{drive.applicationDeadline}</dd>
          </>
        )}
        {drive.driveDate && (
          <>
            <dt>Drive date</dt>
            <dd>{drive.driveDate}</dd>
          </>
        )}
      </dl>

      {drive.rounds.length > 0 && (
        <>
          <h2>Rounds</h2>
          <ol className="round-list">
            {drive.rounds.map((round) => (
              <li key={round.id}>
                {round.name}
                {round.roundDate && <span className="round-date"> — {round.roundDate}</span>}
              </li>
            ))}
          </ol>
        </>
      )}

      <div className="apply-section">
        {!eligible && <p className="field-hint">You don't meet the eligibility criteria for this drive.</p>}
        {drive.status === "CLOSED" && <p className="field-hint">This drive is closed.</p>}
        {applyError && <p className="error-text">{applyError}</p>}
        <button type="button" onClick={handleApply} disabled={!canApply || applying}>
          {applying ? "Applying..." : "Apply"}
        </button>
      </div>
    </div>
  );
}
