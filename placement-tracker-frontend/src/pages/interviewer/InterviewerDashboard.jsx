import { useEffect, useState } from 'react';
import { getMyAssignments, submitInterviewerScore } from '../../api/interviewerService';
import { getErrorMessage } from '../../api/apiError';
import { useToast } from '../../context/useToast';
import StatusBadge from '../../components/common/StatusBadge';
import EmptyState from '../../components/common/EmptyState';
import { LoadingMessage, ErrorMessage } from '../../components/common/StateMessage';

// Each row's score input is bound to that specific assignment - there is no free-text
// student-id or search field anywhere on this page. Selection is exclusively "this row in
// your own assigned list"; the backend independently enforces the same rule server-side.
function ScoreRow({ assignment, busy, error, onSubmit }) {
  const [score, setScore] = useState('');
  const [remarks, setRemarks] = useState('');
  const gradable = assignment.status === 'PENDING' || assignment.status === 'SCORED';

  function handleSubmit(e) {
    e.preventDefault();
    if (score === '') return;
    onSubmit(assignment.roundResultId, Number(score), remarks || null);
  }

  return (
    <tr>
      <td>{assignment.studentName}</td>
      <td className="cell-muted">{assignment.studentEmail}</td>
      <td>
        Round {assignment.roundSequence} — {assignment.roundName}
      </td>
      <td>
        <StatusBadge status={assignment.status} />
      </td>
      <td>
        {gradable ? (
          <form onSubmit={handleSubmit} className="table-actions">
            <input
              type="number"
              step="0.01"
              min="0"
              value={score}
              onChange={(e) => setScore(e.target.value)}
              placeholder="Score"
              disabled={busy}
              style={{ width: 90 }}
            />
            <input
              type="text"
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
              placeholder="Remarks (optional)"
              disabled={busy}
              style={{ minWidth: 160 }}
            />
            <button type="submit" className="btn btn-primary btn-sm" disabled={busy || score === ''}>
              {busy ? 'Submitting...' : 'Submit Score'}
            </button>
          </form>
        ) : (
          <span className="cell-muted">Already graded</span>
        )}
        {error && <div className="field-error">{error}</div>}
      </td>
    </tr>
  );
}

export default function InterviewerDashboard() {
  const showToast = useToast();
  const [assignments, setAssignments] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [busyId, setBusyId] = useState(null);
  const [errorId, setErrorId] = useState(null);
  const [rowError, setRowError] = useState(null);

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    return getMyAssignments()
      .then(setAssignments)
      .catch((err) => setError(getErrorMessage(err, 'Could not load your assigned candidates.')))
      .finally(() => setLoading(false));
  }

  function handleSubmit(roundResultId, score, remarks) {
    setBusyId(roundResultId);
    setErrorId(null);
    setRowError(null);
    submitInterviewerScore(roundResultId, score, remarks)
      .then(() => {
        showToast('Score submitted.', 'success');
        return load();
      })
      .catch((err) => {
        setErrorId(roundResultId);
        setRowError(getErrorMessage(err, 'Could not submit the score.'));
      })
      .finally(() => setBusyId(null));
  }

  if (loading) return <LoadingMessage text="Loading your assigned candidates..." />;
  if (error) return <ErrorMessage text={error} onRetry={load} />;

  return (
    <div>
      <div className="page-header">
        <h1>My Candidates</h1>
      </div>

      {assignments.length === 0 ? (
        <EmptyState
          icon="users"
          title="No candidates assigned yet"
          description="Once the placement team assigns you candidates for a round, they'll appear here."
        />
      ) : (
        <div className="table-card">
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Student</th>
                  <th>Email</th>
                  <th>Round</th>
                  <th>Status</th>
                  <th>Score</th>
                </tr>
              </thead>
              <tbody>
                {assignments.map((a) => (
                  <ScoreRow
                    key={a.id}
                    assignment={a}
                    busy={busyId === a.roundResultId}
                    error={errorId === a.roundResultId ? rowError : null}
                    onSubmit={handleSubmit}
                  />
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
