import { Link } from 'react-router-dom';

// Slide-in quick-preview panel, opened from the "View" button on the Applications table.
// Deliberately shows only fields that are actually present on ApplicationResponse (name,
// email, branch, drive role/company, applied date, status) - the reference design's
// University/Degree/GPA/Skills/Resume fields and "Reject"/"Schedule interview" actions have
// no real data or backend capability behind them in this app, so they're left out rather
// than shown empty or wired to a no-op. Round results and the offer - the real actionable
// content - live on the full detail page, linked below.

function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

export default function ApplicationDetailsPanel({ open, onClose, application }) {
  if (!open || !application) return null;
  const a = application;

  return (
    <div
      className="fixed inset-0 z-40 flex justify-end"
      style={{ background: 'rgba(20,20,15,0.35)' }}
      onClick={onClose}
    >
      <div
        className="w-full max-w-sm bg-white h-full shadow-xl p-6 overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-[17px] font-medium text-black">Application details</h2>
          <button type="button" onClick={onClose} className="text-[#6b6785] text-[13px]" aria-label="Close">
            Close
          </button>
        </div>

        <Section title="Personal information">
          <Row label="Name" value={a.studentName} />
          <Row label="Email" value={a.studentEmail} />
          <Row label="Branch" value={a.studentBranch} />
        </Section>

        <Section title="Applied position">
          <Row label="Role" value={a.drive.role} />
          <Row label="Company" value={a.drive.company.name} />
          <Row label="Applied date" value={formatDate(a.appliedAt)} />
        </Section>

        <Section title="Status">
          <Row label="Application status" value={a.status?.replace('_', ' ')} />
          {a.offer && <Row label="Offer status" value={a.offer.status} />}
        </Section>

        <Link
          to={`/applications/${a.id}`}
          onClick={onClose}
          className="mt-2 block text-center text-[13px] font-medium text-white bg-[#7c5cfc] rounded-lg py-2 hover:bg-[#6d28d9] transition"
        >
          View full details
        </Link>
      </div>
    </div>
  );
}

function Section({ title, children }) {
  return (
    <div className="mb-5 pb-5 border-b border-[#e7e4fa] last:border-0">
      <h3 className="text-[12px] font-medium text-[#6b6785] uppercase tracking-wide mb-2">{title}</h3>
      {children}
    </div>
  );
}

function Row({ label, value }) {
  if (!value) return null;
  return (
    <div className="flex justify-between text-[13px] py-1">
      <span className="text-[#6b6785]">{label}</span>
      <span className="text-black font-medium">{value}</span>
    </div>
  );
}
