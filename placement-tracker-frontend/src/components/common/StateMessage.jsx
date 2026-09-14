export function LoadingMessage({ text = 'Loading...' }) {
  return <div className="state-message">{text}</div>;
}

export function ErrorMessage({ text, onRetry }) {
  return (
    <div className="state-message state-message-error">
      <span>{text}</span>
      {onRetry && (
        <button type="button" className="btn btn-secondary" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  );
}

export function EmptyMessage({ text = 'No records found.' }) {
  return <div className="state-message">{text}</div>;
}
