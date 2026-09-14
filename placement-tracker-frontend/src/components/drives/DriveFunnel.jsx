import { useEffect, useState } from 'react';
import { getDriveFunnel } from '../../api/driveService';
import { getErrorMessage } from '../../api/apiError';
import StatCard from '../common/StatCard';
import { ErrorMessage } from '../common/StateMessage';

export default function DriveFunnel({ driveId }) {
  const [funnel, setFunnel] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  function load() {
    setLoading(true);
    setError(null);
    getDriveFunnel(driveId)
      .then(setFunnel)
      .catch((err) => setError(getErrorMessage(err, 'Could not load the funnel.')))
      .finally(() => setLoading(false));
  }

  // load() mirrors the loadX()-called-from-an-effect pattern used across this codebase's
  // pages (e.g. DriveDetails.jsx) - it's a plain fetch-on-mount helper.
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [driveId]);

  if (loading) return null;
  if (error) return <ErrorMessage text={error} />;
  if (!funnel) return null;

  return (
    <div>
      <div className="stat-grid" style={{ marginBottom: 16 }}>
        <StatCard icon="fileText" variant="arctic" value={funnel.totalApplications} label="Total Applications" />
        <StatCard icon="award" variant="sapphire" value={funnel.offeredCount} label="Offered" />
      </div>

      {funnel.stages.length === 0 ? (
        <p className="cell-muted">No rounds defined yet - the funnel will populate once rounds are added.</p>
      ) : (
        <div className="table-card">
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Round</th>
                  <th>Reached</th>
                  <th>Passed</th>
                  <th>Failed</th>
                  <th>Awaiting</th>
                </tr>
              </thead>
              <tbody>
                {funnel.stages.map((stage) => (
                  <tr key={stage.roundId}>
                    <td>
                      {stage.sequence}. {stage.roundName}
                    </td>
                    <td>{stage.reachedCount}</td>
                    <td>{stage.passedCount}</td>
                    <td>{stage.failedCount}</td>
                    <td>{stage.awaitingCount}</td>
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
