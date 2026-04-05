import { getPasswordStrength } from '@/lib/password-strength';

interface PasswordStrengthBarProps {
  password: string;
}

const colorMap = {
  weak: 'bg-red-500',
  fair: 'bg-yellow-500',
  strong: 'bg-green-500',
};

export default function PasswordStrengthBar({ password }: PasswordStrengthBarProps) {
  const strength = getPasswordStrength(password);

  if (password.length === 0) return null;

  return (
    <div className="space-y-1">
      <div className="h-1.5 w-full rounded-full bg-muted">
        <div
          className={`h-full rounded-full transition-all ${colorMap[strength.level]}`}
          style={{ width: `${strength.percent}%` }}
        />
      </div>
      <p className="text-xs text-muted-foreground">{strength.label}</p>
    </div>
  );
}
