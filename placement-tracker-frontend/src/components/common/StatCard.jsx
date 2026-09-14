import Icon from './Icon';

export default function StatCard({ icon, variant = 'arctic', value, label }) {
  return (
    <div className="stat-card">
      <div className={`stat-card-icon variant-${variant}`}>
        <Icon name={icon} size={20} />
      </div>
      <div className="stat-card-body">
        <div className="stat-card-value">{value}</div>
        <div className="stat-card-label">{label}</div>
      </div>
    </div>
  );
}
