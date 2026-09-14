import Modal from './Modal';
import Icon from './Icon';

// tone: 'danger' (default) for destructive actions (delete, decline) - red warning
// icon and red confirm button. 'primary' for affirmative, non-destructive actions
// (apply, accept) - a neutral check icon and the normal primary button color.
export default function ConfirmDialog({
  title = 'Are you sure?',
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  tone = 'danger',
  busy = false,
  error = null,
  onConfirm,
  onCancel,
}) {
  const isDanger = tone === 'danger';
  return (
    <Modal title={title} onClose={onCancel} width={400}>
      <div className={`confirm-icon${isDanger ? '' : ' tone-primary'}`}>
        <Icon name={isDanger ? 'alertTriangle' : 'checkCircle'} size={22} />
      </div>
      <p className="confirm-message">{message}</p>
      {error && <div className="form-error">{error}</div>}
      <div className="modal-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={busy}>
          {cancelLabel}
        </button>
        <button
          type="button"
          className={`btn ${isDanger ? 'btn-danger' : 'btn-primary'}`}
          onClick={onConfirm}
          disabled={busy}
        >
          {busy ? 'Please wait...' : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
