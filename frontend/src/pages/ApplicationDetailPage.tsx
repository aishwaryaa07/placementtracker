import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api, extractErrorMessage } from "../api/client";
import type { Application, OfferStatus } from "../api/types";
import { StatusBadge } from "../components/StatusBadge";

export function ApplicationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [application, setApplication] = useState<Application | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [offerError, setOfferError] = useState<string | null>(null);
  const [responding, setResponding] = useState(false);

  const load = useCallback(() => {
    return api
      .get<Application>(`/api/student/applications/${id}`)
      .then((response) => setApplication(response.data))
      .catch((err) => setError(extractErrorMessage(err)));
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  const respondToOffer = async (status: OfferStatus) => {
    if (!application?.offer) return;
    setResponding(true);
    setOfferError(null);
    try {
      await api.patch(`/api/student/offers/${application.offer.id}/status`, { status });
      await load();
    } catch (err) {
      setOfferError(extractErrorMessage(err));
    } finally {
      setResponding(false);
    }
  };

  if (error) return <p className="error-text">{error}</p>;
  if (application === null) return <p>Loading application...</p>;

  return (
    <div className="page-card">
      <Link to="/applications" className="back-link">
        &larr; Back to applications
      </Link>
      <div className="drive-card-header">
        <h1>{application.drive.role}</h1>
        <StatusBadge status={application.status} />
      </div>
      <p className="company-name">{application.drive.company.name}</p>
      <p className="deadline">Applied {new Date(application.appliedAt).toLocaleString()}</p>

      {application.roundResults.length > 0 && (
        <>
          <h2>Rounds</h2>
          <ul className="round-result-list">
            {application.roundResults
              .slice()
              .sort((a, b) => a.roundSequence - b.roundSequence)
              .map((result) => (
                <li key={result.id}>
                  <span className="round-name">{result.roundName}</span>
                  <StatusBadge status={result.status} />
                  {result.remarks && <p className="remarks">{result.remarks}</p>}
                </li>
              ))}
          </ul>
        </>
      )}

      {application.offer && (
        <div className="offer-section">
          <h2>Offer</h2>
          <dl className="detail-list">
            <dt>CTC offered</dt>
            <dd>₹{application.offer.ctcOffered.toLocaleString()}</dd>
            <dt>Offer date</dt>
            <dd>{application.offer.offerDate}</dd>
            <dt>Status</dt>
            <dd>
              <StatusBadge status={application.offer.status} />
            </dd>
          </dl>
          {offerError && <p className="error-text">{offerError}</p>}
          {application.offer.status === "PENDING" && (
            <div className="offer-actions">
              <button type="button" onClick={() => respondToOffer("ACCEPTED")} disabled={responding}>
                Accept offer
              </button>
              <button
                type="button"
                className="secondary-button"
                onClick={() => respondToOffer("DECLINED")}
                disabled={responding}
              >
                Decline offer
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
