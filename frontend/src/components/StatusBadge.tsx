const TONE_BY_STATUS: Record<string, "neutral" | "positive" | "negative" | "warning"> = {
  UPCOMING: "neutral",
  ONGOING: "warning",
  CLOSED: "negative",
  APPLIED: "neutral",
  IN_PROGRESS: "warning",
  REJECTED: "negative",
  SELECTED: "positive",
  WITHDRAWN: "negative",
  PENDING: "warning",
  PASSED: "positive",
  FAILED: "negative",
  ACCEPTED: "positive",
  DECLINED: "negative",
};

export function StatusBadge({ status }: { status: string }) {
  const tone = TONE_BY_STATUS[status] ?? "neutral";
  return <span className={`badge badge-${tone}`}>{status.replace("_", " ")}</span>;
}
