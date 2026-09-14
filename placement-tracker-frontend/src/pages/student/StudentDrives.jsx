import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getOpenDrives, applyToDrive } from '../../api/studentDriveService';
import { getMyApplications } from '../../api/studentApplicationService';
import { getErrorMessage } from '../../api/apiError';
import { useToast } from '../../context/useToast';
import PageHeader from '../../components/common/PageHeader';
import EmptyState from '../../components/common/EmptyState';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import StatusBadge from '../../components/common/StatusBadge';
import Icon from '../../components/common/Icon';
import CompanyLogo from '../../components/common/CompanyLogo';
import { ErrorMessage } from '../../components/common/StateMessage';

const DRIVE_STATUSES = ['UPCOMING', 'ONGOING'];

function isPast(dateStr) {
  if (!dateStr) return false;
  return new Date(dateStr) < new Date(new Date().toDateString());
}

function DriveCardSkeleton() {
  return (
    <div className="drive-card-grid">
      {Array.from({ length: 6 }).map((_, i) => (
        <div key={i} className="drive-card">
          <div className="skeleton skeleton-text" style={{ width: '60%', height: 16 }} />
          <div className="skeleton skeleton-text" style={{ width: '40%' }} />
          <div className="skeleton skeleton-text" style={{ width: '80%' }} />
          <div className="skeleton skeleton-text" style={{ width: '50%' }} />
        </div>
      ))}
    </div>
  );
}

export default function StudentDrives() {
  const showToast = useToast();
  const navigate = useNavigate();

  const [drives, setDrives] = useState([]);
  const [appliedDriveIds, setAppliedDriveIds] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [eligibleOnly, setEligibleOnly] = useState(false);

  const [applyTarget, setApplyTarget] = useState(null);
  const [applying, setApplying] = useState(false);
  const [applyError, setApplyError] = useState(null);

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    Promise.all([getOpenDrives(), getMyApplications()])
      .then(([driveList, applications]) => {
        setDrives(driveList);
        setAppliedDriveIds(new Set(applications.map((a) => a.drive.id)));
      })
      .catch((err) => setError(getErrorMessage(err, 'Could not load drives.')))
      .finally(() => setLoading(false));
  }

  const filteredDrives = useMemo(() => {
    const term = search.trim().toLowerCase();
    return drives.filter((d) => {
      if (statusFilter && d.drive.status !== statusFilter) return false;
      if (eligibleOnly && !d.eligible) return false;
      if (term) {
        const haystack = `${d.drive.role} ${d.drive.company.name}`.toLowerCase();
        if (!haystack.includes(term)) return false;
      }
      return true;
    });
  }, [drives, search, statusFilter, eligibleOnly]);

  function openApplyConfirm(driveEntry) {
    setApplyError(null);
    setApplyTarget(driveEntry);
  }

  function confirmApply() {
    setApplying(true);
    setApplyError(null);
    applyToDrive(applyTarget.drive.id)
      .then((application) => {
        setAppliedDriveIds((prev) => new Set(prev).add(applyTarget.drive.id));
        setApplyTarget(null);
        showToast('Application submitted!', 'success');
        navigate(`/student/applications/${application.id}`);
      })
      .catch((err) => setApplyError(getErrorMessage(err, 'Could not submit your application.')))
      .finally(() => setApplying(false));
  }

  return (
    <div>
      <PageHeader title="Browse Drives" subtitle="Explore open placement drives and apply to the ones you're eligible for." />

      <div className="toolbar">
        <div className="search-field">
          <Icon name="search" size={16} />
          <input
            type="text"
            placeholder="Search by role or company..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search drives"
          />
        </div>
        <div className="toolbar-filter">
          <label htmlFor="filter-drive-status">Status</label>
          <select id="filter-drive-status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All</option>
            {DRIVE_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
        <div className="toolbar-filter">
          <label htmlFor="filter-eligible-only">
            <input
              id="filter-eligible-only"
              type="checkbox"
              checked={eligibleOnly}
              onChange={(e) => setEligibleOnly(e.target.checked)}
              style={{ marginRight: 6 }}
            />
            Eligible only
          </label>
        </div>
      </div>

      {loading && <DriveCardSkeleton />}
      {!loading && error && <ErrorMessage text={error} onRetry={load} />}

      {!loading && !error && drives.length === 0 && (
        <EmptyState
          icon="briefcase"
          title="No open drives right now"
          description="Check back soon — new placement drives will appear here as they're scheduled."
        />
      )}

      {!loading && !error && drives.length > 0 && filteredDrives.length === 0 && (
        <EmptyState icon="search" title="No drives match these filters" description="Try adjusting your search or filters." compact />
      )}

      {!loading && !error && filteredDrives.length > 0 && (
        <div className="drive-card-grid">
          {filteredDrives.map((d) => {
            const alreadyApplied = appliedDriveIds.has(d.drive.id);
            const deadlinePassed = isPast(d.drive.applicationDeadline);
            const canApply = d.eligible && !alreadyApplied && !deadlinePassed;

            return (
              <div key={d.drive.id} className="drive-card">
                <div className="drive-card-head">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <CompanyLogo name={d.drive.company.name} logoUrl={d.drive.company.logoUrl} size={36} />
                    <div>
                      <p className="drive-card-role">
                        <Link to={`/student/drives/${d.drive.id}`} className="link-button">
                          {d.drive.role}
                        </Link>
                      </p>
                      <p className="drive-card-company">{d.drive.company.name}</p>
                    </div>
                  </div>
                </div>

                <div className="drive-card-badges">
                  <StatusBadge status={d.drive.status} />
                  <span className={`eligibility-badge ${d.eligible ? 'eligible' : 'not-eligible'}`}>
                    {d.eligible ? 'Eligible' : 'Not Eligible'}
                  </span>
                  {alreadyApplied && <span className="eligibility-badge eligible">Applied</span>}
                </div>

                <div className="drive-card-meta">
                  <div>
                    CTC: <strong>{d.drive.ctc ?? '-'}</strong>
                  </div>
                  <div>
                    Min CGPA: <strong>{d.drive.minCgpa ?? 'Any'}</strong>
                  </div>
                  <div>
                    Apply by: <strong>{d.drive.applicationDeadline || 'No deadline'}</strong>
                  </div>
                </div>

                <div className="drive-card-footer">
                  <Link to={`/student/drives/${d.drive.id}`} className="btn btn-secondary btn-sm">
                    Details
                  </Link>
                  {alreadyApplied ? (
                    <span className="drive-card-note">Already applied</span>
                  ) : deadlinePassed ? (
                    <span className="drive-card-note">Deadline passed</span>
                  ) : !d.eligible ? (
                    <span className="drive-card-note">Not eligible</span>
                  ) : (
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      disabled={!canApply}
                      onClick={() => openApplyConfirm(d)}
                    >
                      Apply
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {applyTarget && (
        <ConfirmDialog
          title="Apply to this drive?"
          message={`Apply for ${applyTarget.drive.role} at ${applyTarget.drive.company.name}?`}
          confirmLabel="Apply"
          tone="primary"
          busy={applying}
          error={applyError}
          onConfirm={confirmApply}
          onCancel={() => setApplyTarget(null)}
        />
      )}
    </div>
  );
}
