export default function StatusBadge({ status }) {
  const cssClass = 'status-badge status-' + String(status).toLowerCase();
  const label = String(status).replace(/_/g, ' ');
  return <span className={cssClass}>{label}</span>;
}
