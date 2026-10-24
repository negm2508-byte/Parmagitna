import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import App from './App';

describe('PaperERP Industrial UI Smoke Test', () => {
  it('renders industrial title and paper manufacturing branding', () => {
    render(<App />);
    expect(screen.getByText('PaperERP Enterprise')).toBeInTheDocument();
    expect(screen.getByText(/مصنع مصر للورق والكرتون • وحدة التحكم الرئيسية PM1/i)).toBeInTheDocument();
  });

  it('renders the industrial gates navigation modules', () => {
    render(<App />);
    expect(screen.getByText(/نظرة عامة على صالة الإنتاج/i)).toBeInTheDocument();
    expect(screen.getByText(/حاسبة تقطيع الرولات/i)).toBeInTheDocument();
    expect(screen.getByText(/شجرة نسب الرول/i)).toBeInTheDocument();
    expect(screen.getByText(/مختبر فحص الجودة/i)).toBeInTheDocument();
    expect(screen.getByText(/طباعة الباركود الحراري/i)).toBeInTheDocument();
    expect(screen.getByText(/سجل حركات المخزن/i)).toBeInTheDocument();
  });

  it('displays the local factory LAN address and database status', () => {
    render(<App />);
    expect(screen.getByText(/LAN 192.168.9.223/i)).toBeInTheDocument();
    expect(screen.getByText(/Supabase PostgreSQL|Local Persistence/i)).toBeInTheDocument();
  });
});
