import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getMyApplication } from '../../api/studentApplicationService';
import { respondToOffer } from '../../api/studentOfferService';
import { submitRoundScore } from '../../api/studentRoundResultService';
import { getErrorMessage } from '../../api/apiError';
import { useToast } from '../../context/useToast';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import EmptyState from '../../components/common/EmptyState';
import Icon from '../../components/common/Icon';
import CompanyLogo from '../../components/common/CompanyLogo';
import { LoadingMessage, ErrorMessage } from '../../components/common/StateMessage';

function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

// A locked round has no real result yet regardless of what its PENDING placeholder says,
// so it's treated as its own status here rather than shown as "pending".
function stepStatusKey(roundResult) {
  return roundResult.locked ? 'locked' : roundResult.status.toLowerCase();
}

function stepIconName(roundResult) {
  if (roundResult.locked) return 'lock';
  if (roundResult.status === 'PASSED') return 'checkCircle';
  if (roundResult.status === 'FAILED') return 'xCircle';
  return 'clock';
}

// Shown only for a round self-score-submission is possible for: a THRESHOLD round with a
// minimum score configured, or any TOP_N / THRESHOLD_THEN_TOP_N round (ranking decides the
// outcome later, at Finalize, rather than needing a per-round minimum). Otherwise the round
// is graded by the placement team instead, and this never renders.
function ScoreSubmitForm({ selectionMode, minScore, busy, error, onSubmit }) {
  const [score, setScore] = useState('');

  function handleSubmit(e) {
    e.preventDefault();
    if (score === '') return;
    onSubmit(Number(score));
  }

  const isRankingMode = selectionMode === 'TOP_N' || selectionMode === 'THRESHOLD_THEN_TOP_N';
  const label = isRankingMode
    ? minScore != null
      ? `Your score (must be at least ${minScore} to be considered)`
      : 'Your score'
    : `Your score (minimum ${minScore} to pass)`;
  const helpText = isRankingMode
    ? 'Results for this round are decided once the placement team ranks every candidate - your status will update after that.'
    : null;

  return (
    <form className="score-submit-form" onSubmit={handleSubmit}>
      <label htmlFor="round-score-input">
        {label}
        <span className="required-mark">*</span>
      </label>
      {helpText && <div className="field-hint">{helpText}</div>}
      <div className="table-actions">
        <input
          id="round-score-input"
          type="number"
          step="0.01"
          min="0"
          value={score}
          onChange={(e) => setScore(e.target.value)}
          disabled={busy}
          placeholder="e.g. 72"
          style={{ maxWidth: 140 }}
        />
        <button type="submit" className="btn btn-primary btn-sm" disabled={busy || score === ''}>
          {busy ? 'Submitting...' : 'Submit Score'}
        </button>
      </div>
      {error && <div className="field-error">{error}</div>}
    </form>
  );
}

export default function StudentApplicationDetails() {
  const { id } = useParams();
  const showToast = useToast();

  const [application, setApplication] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [respondAction, setRespondAction] = useState(null); // 'ACCEPTED' | 'DECLINED' | null
  const [responding, setResponding] = useState(false);
  const [respondError, setRespondError] = useState(null);

  const [scoreBusyId, setScoreBusyId] = useState(null);
  const [scoreErrorId, setScoreErrorId] = useState(null);
  const [scoreError, setScoreError] = useState(null);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  function load() {
    setLoading(true);
    setError(null);
    return getMyApplication(id)
      .then(setApplication)
      .catch((err) => setError(getErrorMessage(err, 'Could not load this application.')))
      .finally(() => setLoading(false));
  }

  function handleScoreSubmit(roundResultId, score) {
    setScoreBusyId(roundResultId);
    setScoreErrorId(null);
    setScoreError(null);
    submitRoundScore(roundResultId, score)
      .then((updated) => {
        showToast(
          updated.status === 'PASSED' ? 'Score submitted - you passed!' : 'Score submitted - not enough to pass.',
          updated.status === 'PASSED' ? 'success' : 'error'
        );
        return load();
      })
      .catch((err) => {
        setScoreErrorId(roundResultId);
        setScoreError(getErrorMessage(err, 'Could not submit your score.'));
      })
      .finally(() => setScoreBusyId(null));
  }

  function confirmRespond() {
    setResponding(true);
    setRespondError(null);
    respondToOffer(application.offer.id, respondAction)
      .then(() => {
        showToast(respondAction === 'ACCEPTED' ? 'Offer accepted!' : 'Offer declined.', 'success');
        setRespondAction(null);
        return load();
      })
      .catch((err) => setRespondError(getErrorMessage(err, 'Could not update the offer.')))
      .finally(() => setResponding(false));
  }

  if (loading) return <LoadingMessage text="Loading application..." />;
  if (error) return <ErrorMessage text={error} onRetry={load} />;
  if (!application) return null;

  return (
    <div>
      <div className="page-header">
        <div>
          <Link to="/student/applications" className="back-link">
            <Icon name="arrowLeft" size={14} />
            Back to Applications
          </Link>
          <h1 className="company-name-cell">
            <CompanyLogo name={application.drive.company.name} logoUrl={application.drive.company.logoUrl} size={32} />
            {application.drive.role} @ {application.drive.company.name}
          </h1>
        </div>
      </div>

      <div className="detail-card">
        <dl className="detail-list">
          <dt>Status</dt>
          <dd>
            <StatusBadge status={application.status} />
          </dd>

          <dt>Applied On</dt>
          <dd>{formatDate(application.appliedAt)}</dd>

          <dt>CTC</dt>
          <dd>{application.drive.ctc ?? '-'}</dd>

          <dt>Drive Date</dt>
          <dd>{application.drive.driveDate || '-'}</dd>
        </dl>
      </div>

      <h2 className="section-heading">Selection Process</h2>

      {application.roundResults.length === 0 ? (
        <EmptyState compact icon="fileText" title="No round results yet" description="Round results will appear here as the drive progresses." />
      ) : (
        <div className="selection-process">
          {[...application.roundResults].sort((a, b) => a.roundSequence - b.roundSequence).map((roundResult) => (
            <div key={roundResult.id} className={`selection-step${roundResult.locked ? ' is-locked' : ''}`}>
              <div className={`selection-step-icon status-${stepStatusKey(roundResult)}`}>
                <Icon name={stepIconName(roundResult)} size={18} />
              </div>
              <div className="selection-step-body">
                <div className="selection-step-title">
                  Round {roundResult.roundSequence} — {roundResult.roundName}
                </div>
                <StatusBadge status={roundResult.locked ? 'LOCKED' : roundResult.status} />
                {roundResult.roundDate && (
                  <div className="selection-step-remarks">Scheduled: {formatDate(roundResult.roundDate)}</div>
                )}
                {roundResult.roundDescription && (
                  <div className="selection-step-remarks">{roundResult.roundDescription}</div>
                )}
                {!roundResult.locked && roundResult.remarks && (
                  <div className="selection-step-remarks">{roundResult.remarks}</div>
                )}
                {!roundResult.locked && roundResult.score != null && (
                  <div className="selection-step-remarks">Your submitted score: {roundResult.score}</div>
                )}
                {!roundResult.locked &&
                  roundResult.status === 'PENDING' &&
                  roundResult.score == null &&
                  (roundResult.selectionMode !== 'THRESHOLD' || roundResult.minScore != null) && (
                    <ScoreSubmitForm
                      selectionMode={roundResult.selectionMode}
                      minScore={roundResult.minScore}
                      busy={scoreBusyId === roundResult.id}
                      error={scoreErrorId === roundResult.id ? scoreError : null}
                      onSubmit={(score) => handleScoreSubmit(roundResult.id, score)}
                    />
                  )}
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="section-heading-row">
        <h2 className="section-heading">Offer</h2>
      </div>

      {respondError && <ErrorMessage text={respondError} />}

      {application.offer ? (
        <div className="summary-card">
          <div>
            <div className="summary-card-label">CTC Offered</div>
            <div className="summary-card-value">{application.offer.ctcOffered}</div>
          </div>
          <div>
            <div className="summary-card-label">Offer Date</div>
            <div className="summary-card-value">{application.offer.offerDate || '-'}</div>
          </div>
          <div>
            <div className="summary-card-label">Status</div>
            <div>
              <StatusBadge status={application.offer.status} />
            </div>
          </div>
          {application.offer.status === 'PENDING' && (
            <div className="table-actions">
              <button type="button" className="btn btn-primary btn-sm" onClick={() => setRespondAction('ACCEPTED')}>
                Accept
              </button>
              <button type="button" className="btn btn-danger btn-sm" onClick={() => setRespondAction('DECLINED')}>
                Decline
              </button>
            </div>
          )}
        </div>
      ) : (
        <EmptyState compact icon="star" title="No offer yet" description="If you're selected, your offer will appear here." />
      )}

      {respondAction && (
        <ConfirmDialog
          title={respondAction === 'ACCEPTED' ? 'Accept this offer?' : 'Decline this offer?'}
          message={
            respondAction === 'ACCEPTED'
              ? `Accept the offer of ${application.offer.ctcOffered} from ${application.drive.company.name}?`
              : `Decline the offer from ${application.drive.company.name}? This cannot be undone.`
          }
          confirmLabel={respondAction === 'ACCEPTED' ? 'Accept Offer' : 'Decline Offer'}
          tone={respondAction === 'ACCEPTED' ? 'primary' : 'danger'}
          busy={responding}
          error={respondError}
          onConfirm={confirmRespond}
          onCancel={() => setRespondAction(null)}
        />
      )}
    </div>
  );
}
