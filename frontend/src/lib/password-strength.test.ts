import { describe, it, expect } from 'vitest';
import { getPasswordStrength } from './password-strength';

describe('getPasswordStrength', () => {
  it('returns empty label for empty password', () => {
    const result = getPasswordStrength('');
    expect(result.level).toBe('weak');
    expect(result.label).toBe('');
    expect(result.percent).toBe(0);
  });

  it('returns weak for short simple password', () => {
    const result = getPasswordStrength('abc');
    expect(result.level).toBe('weak');
  });

  it('returns weak for 8-char lowercase only', () => {
    const result = getPasswordStrength('abcdefgh');
    expect(result.level).toBe('weak');
  });

  it('returns fair for mixed case with length', () => {
    const result = getPasswordStrength('Abcdefgh1');
    expect(result.level).toBe('fair');
  });

  it('returns strong for long password with all character types', () => {
    const result = getPasswordStrength('MyP@ssw0rd123');
    expect(result.level).toBe('strong');
    expect(result.percent).toBe(100);
  });
});
