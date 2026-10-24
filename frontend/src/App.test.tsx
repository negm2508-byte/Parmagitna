import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import App from './App';

describe('PaperERP Frontend Smoke Test', () => {
  it('renders application header and title in Arabic RTL', () => {
    render(<App />);
    expect(screen.getByText('PaperERP Enterprise')).toBeInTheDocument();
    expect(screen.getByText(/نظام إدارة مصانع وتصنيع الورق/i)).toBeInTheDocument();
  });

  it('renders developer ownership areas correctly', () => {
    render(<App />);
    expect(screen.getByText(/المطور \(A\)/i)).toBeInTheDocument();
    expect(screen.getByText(/المطور \(B\)/i)).toBeInTheDocument();
    expect(screen.getByText(/المطور \(C\)/i)).toBeInTheDocument();
  });

  it('renders connection check button', () => {
    render(<App />);
    expect(screen.getByRole('button', { name: /فحص الاتصال/i })).toBeInTheDocument();
  });
});
