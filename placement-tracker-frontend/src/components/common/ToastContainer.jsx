import { useContext } from 'react';
import { ToastContext } from '../../context/toast-context';
import Icon from './Icon';

export default function ToastContainer() {
  const context = useContext(ToastContext);
  if (!context || context.toasts.length === 0) return null;

  return (
    <div className="toast-stack">
      {context.toasts.map((toast) => (
        <div key={toast.id} className={`toast toast-${toast.type}`}>
          <span className="toast-icon">
            <Icon name={toast.type === 'error' ? 'alertCircle' : 'checkCircle'} size={17} />
          </span>
          <span>{toast.message}</span>
          <button
            type="button"
            className="toast-dismiss"
            aria-label="Dismiss"
            onClick={() => context.dismiss(toast.id)}
          >
            <Icon name="close" size={14} />
          </button>
        </div>
      ))}
    </div>
  );
}
