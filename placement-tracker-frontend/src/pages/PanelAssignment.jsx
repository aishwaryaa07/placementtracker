import { useEffect, useState } from 'react';
import { useParams, useLocation, Link } from 'react-router-dom';
import { listInterviewers, createInterviewer, getPanelAssignments, getCandidatePool, assignPanel, unassignPanel } from '../api/panelAssignmentService';
import { getErrorMessage } from '../api/apiError';
import { useToast } from '../context/useToast';
import Modal from '../components/common/Modal';
import ConfirmDialog from '../components/common/ConfirmDialog';
import StatusBadge from '../components/common/StatusBadge';
import EmptyState from '../components/common/EmptyState';
import Icon from '../components/common/Icon';
import { LoadingMessage, ErrorMessage } from '../components/common/StateMessage';

function CreateInterviewerForm({ submitting, formError, onSubmit, onCancel }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  function handleSubmit(e) {
    e.preventDefault();
    onSubmit(name.trim(), email.trim(), password);
  }

  return (
    <form onSubmit={handleSubmit}>
      {formError && <div className="form-error">{formError}</div>}
      <label htmlFor="interviewer-name">
        Name<span className="required-mark">*</span>
      </label>
      <input id="interviewer-name" value={name} onChange={(e) => setName(e.target.value)} required />

      <label htmlFor="new-interviewer-email">
        Email<span className="required-mark">*</span>
      </label>
      <input id="new-interviewer-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />

      <label htmlFor="new-interviewer-password">
        Password<span className="required-mark">*</span>
      </label>
      <input
        id="new-interviewer-password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        minLength={8}
        required
      />

      <div className="modal-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Creating...' : 'Create Interviewer'}
        </button>
      </div>
    </form>
  );
}

export default function PanelAssignment() {
  const { roundId } = useParams();
  const location = useLocation();
  const showToast = useToast();
  const roundName = location.state?.roundName;
  const driveId = location.state?.driveId;

  const [interviewers, setInterviewers] = useState([]);
  const [candidates, setCandidates] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [selectedInterviewerId, setSelectedInterviewerId] = useState('');
  const [selectedCandidateIds, setSelectedCandidateIds] = useState([]);
  const [assigning, setAssigning] = useState(false);
  const [assignError, setAssignError] = useState(null);

  const [createOpen, setCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState(null);

  const [unassignTarget, setUnassignTarget] = useState(null);
  const [unassigning, setUnassigning] = useState(false);
  const [unassignError, setUnassignError] = useState(null);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roundId]);

  function load() {
    setLoading(true);
    setError(null);
    Promise.all([listInterviewers(), getCandidatePool(roundId), getPanelAssignments(roundId)])
      .then(([iv, cand, assign]) => {
        setInterviewers(iv);
        setCandidates(cand);
        setAssignments(assign);
      })
      .catch((err) => setError(getErrorMessage(err, 'Could not load panel assignment data.')))
      .finally(() => setLoading(false));
  }

  function toggleCandidate(id) {
    setSelectedCandidateIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));
  }

  function handleAssign() {
    if (!selectedInterviewerId || selectedCandidateIds.length === 0) return;
    setAssigning(true);
    setAssignError(null);
    assignPanel(roundId, Number(selectedInterviewerId), selectedCandidateIds)
      .then(() => {
        showToast('Candidates assigned.', 'success');
        setSelectedCandidateIds([]);
        return load();
      })
      .catch((err) => setAssignError(getErrorMessage(err, 'Could not assign candidates.')))
      .finally(() => setAssigning(false));
  }

  function handleCreateInterviewer(name, email, password) {
    setCreating(true);
    setCreateError(null);
    createInterviewer(name, email, password)
      .then((created) => {
        setCreateOpen(false);
        showToast('Interviewer account created.', 'success');
        setSelectedInterviewerId(String(created.id));
        return load();
      })
      .catch((err) => setCreateError(getErrorMessage(err, 'Could not create interviewer account.')))
      .finally(() => setCreating(false));
  }

  function confirmUnassign() {
    setUnassigning(true);
    setUnassignError(null);
    unassignPanel(unassignTarget.id)
      .then(() => {
        setUnassignTarget(null);
        showToast('Assignment removed.', 'success');
        return load();
      })
      .catch((err) => setUnassignError(getErrorMessage(err, 'Could not remove assignment.')))
      .finally(() => setUnassigning(false));
  }

  if (loading) return <LoadingMessage text="Loading panel assignment data..." />;
  if (error) return <ErrorMessage text={error} onRetry={load} />;

  const assignedRoundResultIds = new Set(assignments.map((a) => a.roundResultId));

  return (
    <div>
      <div className="page-header">
        <div>
          {driveId && (
            <Link to={`/drives/${driveId}`} className="back-link">
              <Icon name="arrowLeft" size={14} />
              Back to Drive
            </Link>
          )}
          <h1>Panel Assignment{roundName ? ` — ${roundName}` : ''}</h1>
        </div>
      </div>

      <div className="detail-card">
        <h2 className="section-heading" style={{ marginTop: 0 }}>
          Assign candidates to an interviewer
        </h2>

        <label htmlFor="interviewer-select">Interviewer</label>
        <div className="table-actions" style={{ marginBottom: 12 }}>
          <select
            id="interviewer-select"
            value={selectedInterviewerId}
            onChange={(e) => setSelectedInterviewerId(e.target.value)}
            style={{ minWidth: 240 }}
          >
            <option value="">Select an interviewer...</option>
            {interviewers.map((iv) => (
              <option key={iv.id} value={iv.id}>
                {iv.name} ({iv.email})
              </option>
            ))}
          </select>
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => setCreateOpen(true)}>
            <Icon name="plus" size={14} />
            New Interviewer
          </button>
        </div>

        {candidates.length === 0 ? (
          <EmptyState compact icon="users" title="No candidates in this round yet" description="Candidates appear here once students reach this round." />
        ) : (
          <div className="table-card">
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th></th>
                    <th>Student</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {candidates.map((c) => (
                    <tr key={c.roundResultId} style={c.locked || c.alreadyDecided ? { opacity: 0.5 } : undefined}>
                      <td>
                        <input
                          type="checkbox"
                          checked={selectedCandidateIds.includes(c.roundResultId)}
                          disabled={c.locked || c.alreadyDecided}
                          onChange={() => toggleCandidate(c.roundResultId)}
                        />
                      </td>
                      <td>
                        {c.studentName}
                        <div className="cell-muted">{c.studentEmail}</div>
                      </td>
                      <td>
                        <StatusBadge status={c.locked ? 'LOCKED' : c.status} />
                        {c.alreadyDecided && (
                          <span className="cell-muted" style={{ marginLeft: 6 }}>
                            (already graded)
                          </span>
                        )}
                        {assignedRoundResultIds.has(c.roundResultId) && (
                          <span className="cell-muted" style={{ marginLeft: 6 }}>
                            (assigned)
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {assignError && <ErrorMessage text={assignError} />}
        <div className="modal-actions" style={{ justifyContent: 'flex-start', marginTop: 12 }}>
          <button
            type="button"
            className="btn btn-primary"
            disabled={assigning || !selectedInterviewerId || selectedCandidateIds.length === 0}
            onClick={handleAssign}
          >
            {assigning ? 'Assigning...' : `Assign Selected (${selectedCandidateIds.length})`}
          </button>
        </div>
      </div>

      <h2 className="section-heading">Current Assignments</h2>
      {assignments.length === 0 ? (
        <EmptyState compact icon="users" title="No assignments yet" description="Assign candidates to an interviewer above." />
      ) : (
        <div className="table-card">
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Interviewer</th>
                  <th>Student</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {assignments.map((a) => (
                  <tr key={a.id}>
                    <td>{a.interviewerName}</td>
                    <td>
                      {a.studentName}
                      <div className="cell-muted">{a.studentEmail}</div>
                    </td>
                    <td>
                      <StatusBadge status={a.status} />
                    </td>
                    <td className="table-actions">
                      <button
                        type="button"
                        className="btn btn-danger btn-sm"
                        onClick={() => {
                          setUnassignError(null);
                          setUnassignTarget(a);
                        }}
                      >
                        Remove
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {createOpen && (
        <Modal title="Create Interviewer Account" onClose={() => setCreateOpen(false)}>
          <CreateInterviewerForm submitting={creating} formError={createError} onSubmit={handleCreateInterviewer} onCancel={() => setCreateOpen(false)} />
        </Modal>
      )}

      {unassignTarget && (
        <ConfirmDialog
          title="Remove Assignment"
          message={`Remove ${unassignTarget.interviewerName}'s assignment to ${unassignTarget.studentName}?`}
          confirmLabel="Remove"
          busy={unassigning}
          error={unassignError}
          onConfirm={confirmUnassign}
          onCancel={() => setUnassignTarget(null)}
        />
      )}
    </div>
  );
}
