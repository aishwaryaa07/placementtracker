import { useEffect, useMemo, useState } from 'react';
import { getAllStudents } from '../api/adminStudentService';
import { getErrorMessage } from '../api/apiError';
import PageHeader from '../components/common/PageHeader';
import EmptyState from '../components/common/EmptyState';
import TableSkeleton from '../components/common/TableSkeleton';
import Icon from '../components/common/Icon';
import { ErrorMessage } from '../components/common/StateMessage';

export default function CandidateProfiles() {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [search, setSearch] = useState('');
  const [branchFilter, setBranchFilter] = useState('');

  useEffect(() => {
    load();
  }, []);

  function load() {
    setLoading(true);
    setError(null);
    getAllStudents()
      .then(setStudents)
      .catch((err) => setError(getErrorMessage(err, 'Could not load candidate profiles.')))
      .finally(() => setLoading(false));
  }

  const branches = useMemo(() => [...new Set(students.map((s) => s.branch).filter(Boolean))].sort(), [students]);

  const filteredStudents = useMemo(() => {
    const term = search.trim().toLowerCase();
    return students.filter((s) => {
      if (branchFilter && s.branch !== branchFilter) return false;
      if (term && !`${s.name} ${s.email}`.toLowerCase().includes(term)) return false;
      return true;
    });
  }, [students, search, branchFilter]);

  return (
    <div>
      <PageHeader title="Candidate Profiles" subtitle="Browse students who have completed their placement profile." />

      {!loading && !error && students.length > 0 && (
        <div className="toolbar">
          <div className="search-field">
            <Icon name="search" size={16} />
            <input
              type="text"
              placeholder="Search by name or email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Search candidates"
            />
          </div>
          <div className="toolbar-filter">
            <label htmlFor="filter-candidate-branch">Branch</label>
            <select id="filter-candidate-branch" value={branchFilter} onChange={(e) => setBranchFilter(e.target.value)}>
              <option value="">All</option>
              {branches.map((b) => (
                <option key={b} value={b}>
                  {b}
                </option>
              ))}
            </select>
          </div>
        </div>
      )}

      {loading && <TableSkeleton rows={5} columns={8} />}
      {!loading && error && <ErrorMessage text={error} onRetry={load} />}

      {!loading && !error && students.length === 0 && (
        <EmptyState
          icon="user"
          title="No candidate profiles yet"
          description="Students will show up here once they complete their placement profile."
        />
      )}

      {!loading && !error && students.length > 0 && filteredStudents.length === 0 && (
        <EmptyState icon="search" title="No matching candidates" description="Try adjusting your search or filter." compact />
      )}

      {!loading && !error && filteredStudents.length > 0 && (
        <div className="table-card">
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Branch</th>
                  <th>Graduation Year</th>
                  <th>CGPA</th>
                  <th>Phone</th>
                  <th>Resume</th>
                  <th>10th Marksheet</th>
                  <th>12th Marksheet</th>
                </tr>
              </thead>
              <tbody>
                {filteredStudents.map((s) => (
                  <tr key={s.id}>
                    <td>
                      {s.name}
                      <div className="cell-muted">{s.email}</div>
                    </td>
                    <td>{s.branch || '-'}</td>
                    <td>{s.graduationYear ?? '-'}</td>
                    <td>{s.cgpa ?? '-'}</td>
                    <td className="cell-muted">{s.phone || '-'}</td>
                    <td>
                      {s.resumeUrl ? (
                        <a href={s.resumeUrl} target="_blank" rel="noreferrer" className="link-button">
                          View
                        </a>
                      ) : (
                        <span className="cell-muted">-</span>
                      )}
                    </td>
                    <td>
                      {s.tenthMarksheetUrl ? (
                        <a href={s.tenthMarksheetUrl} target="_blank" rel="noreferrer" className="link-button">
                          View
                        </a>
                      ) : (
                        <span className="cell-muted">-</span>
                      )}
                    </td>
                    <td>
                      {s.twelfthMarksheetUrl ? (
                        <a href={s.twelfthMarksheetUrl} target="_blank" rel="noreferrer" className="link-button">
                          View
                        </a>
                      ) : (
                        <span className="cell-muted">-</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
