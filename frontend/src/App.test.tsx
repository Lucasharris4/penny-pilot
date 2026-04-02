import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect } from 'vitest';
import App from './App';

describe('App', () => {
  it('renders login page for unauthenticated users', () => {
    localStorage.removeItem('token');
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <App />
      </MemoryRouter>
    );
    expect(screen.getByText('Sign in to your account')).toBeInTheDocument();
  });

  it('renders register page', () => {
    localStorage.removeItem('token');
    render(
      <MemoryRouter initialEntries={['/register']}>
        <App />
      </MemoryRouter>
    );
    expect(screen.getByText('Create Account')).toBeInTheDocument();
  });
});
