export type StrengthLevel = 'weak' | 'fair' | 'strong';

export interface PasswordStrength {
  level: StrengthLevel;
  label: string;
  percent: number;
}

export function getPasswordStrength(password: string): PasswordStrength {
  if (password.length === 0) {
    return { level: 'weak', label: '', percent: 0 };
  }

  let score = 0;
  if (password.length >= 8) score++;
  if (password.length >= 12) score++;
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[^a-zA-Z0-9]/.test(password)) score++;

  if (score <= 2) return { level: 'weak', label: 'Weak', percent: 33 };
  if (score <= 3) return { level: 'fair', label: 'Fair', percent: 66 };
  return { level: 'strong', label: 'Strong', percent: 100 };
}
