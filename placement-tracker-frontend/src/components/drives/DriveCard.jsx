// Drive card - matches the reference "inspiration" card layout: title + status badge on one
// line, tagline, divider, then a 2x2 grid of colored stat chips instead of plain text rows.
//
// Presentational only - stats/handlers are passed in as props by the caller (Drives.jsx).
// onEdit/onDelete are optional small icon actions added alongside the given "View details"
// footer button, so switching from the old table to this card grid doesn't drop any existing
// admin capability.
//
// Colors match the app's re-themed palette (index.css :root) - purple/navy/lavender - rather
// than duplicating the CSS custom properties (Tailwind utility classes can't reference them
// directly), so this card visually matches the rest of the re-themed admin app.

const chipColors = {
  blue: 'bg-[#e3eef7] text-[#175480]',
  purple: 'bg-[#ede7fe] text-[#6d28d9]',
  amber: 'bg-[#fceeda] text-[#b45309]',
  green: 'bg-[#d7f5df] text-[#146c2e]',
};

const statusColors = {
  ONGOING: 'bg-[#d7f5df] text-[#146c2e]',
  CLOSED: 'bg-[#ede9f5] text-[#6b6785]',
  UPCOMING: 'bg-[#fceeda] text-[#b45309]',
};

const approvalColors = {
  DRAFT: 'bg-[#ede9f5] text-[#6b6785]',
  PENDING_APPROVAL: 'bg-[#fef3c7] text-[#92400e]',
  REJECTED: 'bg-[#fde2e1] text-[#9f1c1c]',
};

export default function DriveCard({
  title,
  tagline,
  status = 'ONGOING',
  approvalStatus,
  createdByName,
  rejectionReason,
  onApprove,
  onReject,
  stats = [],
  onView,
  onEdit,
  onDelete,
}) {
  // APPROVED drives (the normal case - every admin-created drive, plus every interviewer
  // drive once signed off) show nothing extra here; only a non-final approval state earns
  // a banner, so the common case doesn't get cluttered with a redundant "Approved" pill.
  const showApprovalBanner = approvalStatus && approvalStatus !== 'APPROVED';

  return (
    <div className="bg-white rounded-2xl border border-[#e7e4fa] shadow-sm p-5 w-full">
      {showApprovalBanner && (
        <div className={`rounded-lg px-3 py-2 mb-3 text-[12px] ${approvalColors[approvalStatus] || approvalColors.DRAFT}`}>
          <div className="flex items-center justify-between gap-2">
            <span className="font-medium">
              {approvalStatus === 'PENDING_APPROVAL' && `Pending approval${createdByName ? ` — ${createdByName}` : ''}`}
              {approvalStatus === 'DRAFT' && `Draft${createdByName ? ` — ${createdByName}` : ''} (not yet submitted)`}
              {approvalStatus === 'REJECTED' && 'Rejected'}
            </span>
            {approvalStatus === 'PENDING_APPROVAL' && (onApprove || onReject) && (
              <div className="flex gap-1.5 flex-shrink-0">
                {onApprove && (
                  <button
                    type="button"
                    onClick={onApprove}
                    className="text-[11px] font-medium bg-[#146c2e] text-white rounded-full px-2.5 py-1 hover:opacity-90 transition"
                  >
                    Approve
                  </button>
                )}
                {onReject && (
                  <button
                    type="button"
                    onClick={onReject}
                    className="text-[11px] font-medium bg-[#9f1c1c] text-white rounded-full px-2.5 py-1 hover:opacity-90 transition"
                  >
                    Reject
                  </button>
                )}
              </div>
            )}
          </div>
          {approvalStatus === 'REJECTED' && rejectionReason && <div className="mt-1 opacity-90">Reason: {rejectionReason}</div>}
        </div>
      )}

      <div className="flex items-center justify-between gap-2">
        <h3 className="text-[16px] font-medium text-black">{title}</h3>
        <div className="flex items-center gap-1.5 flex-shrink-0">
          <span
            className={`text-[11px] font-medium px-2.5 py-1 rounded-full whitespace-nowrap ${
              statusColors[status] || statusColors.ONGOING
            }`}
          >
            {status}
          </span>
          {onEdit && (
            <button
              type="button"
              onClick={onEdit}
              aria-label="Edit drive"
              className="text-[11px] font-medium text-[#6b6785] border border-[#c9bff5] rounded-full px-2 py-1 hover:bg-[#f6f3ff] transition"
            >
              Edit
            </button>
          )}
          {onDelete && (
            <button
              type="button"
              onClick={onDelete}
              aria-label="Delete drive"
              className="text-[11px] font-medium text-[#b23b3b] border border-[#e8b4b4] rounded-full px-2 py-1 hover:bg-[#fbeeee] transition"
            >
              Delete
            </button>
          )}
        </div>
      </div>

      {tagline && <p className="text-[13px] text-[#6b6785] mt-2 leading-relaxed">{tagline}</p>}

      <div className="border-t border-[#e7e4fa] my-4" />

      <div className="grid grid-cols-2 gap-3">
        {stats.map((s) => (
          <div key={s.label} className={`rounded-xl px-3 py-2.5 ${chipColors[s.color] || chipColors.blue}`}>
            <div className="text-[18px] font-medium leading-tight">{s.value}</div>
            <div className="text-[12px] opacity-90">{s.label}</div>
          </div>
        ))}
      </div>

      {onView && (
        <button
          type="button"
          onClick={onView}
          className="mt-4 w-full text-[13px] font-medium border border-[#c9bff5] rounded-lg py-2 hover:bg-[#f6f3ff] transition"
        >
          View details
        </button>
      )}
    </div>
  );
}
