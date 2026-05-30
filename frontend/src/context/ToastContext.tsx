import { createContext, useCallback, useContext, useRef, useState } from 'react';
import type { ReactNode } from 'react';

type ToastVariant = 'success' | 'error';
type ToastPosition = 'bottom-right' | 'bottom-left' | 'bottom-center';

interface ToastItem {
  id: number;
  message: string;
  variant: ToastVariant;
}

interface ToastContextValue {
  showToast: (message: string, variant?: ToastVariant) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

const POSITION_CLASSES: Record<ToastPosition, string> = {
  'bottom-right': 'bottom-4 right-4 items-end',
  'bottom-left': 'bottom-4 left-4 items-start',
  'bottom-center': 'bottom-4 left-1/2 -translate-x-1/2 items-center',
};

export const TOAST_POSITION: ToastPosition = 'bottom-right';

const DISMISS_MS = 2500;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const nextId = useRef(0);

  const showToast = useCallback((message: string, variant: ToastVariant = 'success') => {
    const id = nextId.current++;
    setToasts(prev => [...prev, { id, message, variant }]);
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), DISMISS_MS);
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className={`fixed z-50 flex flex-col gap-2 pointer-events-none ${POSITION_CLASSES[TOAST_POSITION]}`}>
        {toasts.map(t => (
          <div
            key={t.id}
            className={[
              'pointer-events-auto px-4 py-2.5 rounded-md shadow-lg text-sm font-medium',
              t.variant === 'error'
                ? 'bg-destructive text-destructive-foreground'
                : 'bg-green-600 text-white',
            ].join(' ')}
          >
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
