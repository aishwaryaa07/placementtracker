import Icon from './Icon';

export default function EmptyState({
  icon = 'inbox',
  title,
  description,
  actionLabel,
  onAction,
  compact = false,
}) {
  return (
    <div className={`empty-state${compact ? ' compact' : ''}`}>
      <div className="empty-state-icon">
        <Icon name={icon} size={24} />
      </div>
      <p className="empty-state-title">{title}</p>
      {description && <p className="empty-state-description">{description}</p>}
      {actionLabel && onAction && (
        <button type="button" className="btn btn-primary" onClick={onAction}>
          <Icon name="plus" size={16} />
          {actionLabel}
        </button>
      )}
    </div>
  );
}
