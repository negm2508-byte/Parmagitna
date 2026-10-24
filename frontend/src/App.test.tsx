import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import App from './App';

describe('PaperERP Industrial UI Smoke Test', () => {
  it('renders industrial title and paper manufacturing branding', () => {
    render(<App />);
    expect(screen.getByText('PaperERP Enterprise')).toBeInTheDocument();
    expect(screen.getByText(/نظام تتبع رولات الورق/i)).toBeInTheDocument();
  });

  it('renders the interactive slitting calculator tab by default', () => {
    render(<App />);
    expect(screen.getByText(/حاسبة تقطيع الرولات وميزان الكتلة/i)).toBeInTheDocument();
    expect(screen.getByText(/مخطط وضع سكاكين المقص الآلي/i)).toBeInTheDocument();
    expect(screen.getByText(/اعتماد التقطيع الذري وتوليد الرولات/i)).toBeInTheDocument();
  });

  it('displays the local factory LAN address', () => {
    render(<App />);
    expect(screen.getByText(/192.168.9.223:5173/i)).toBeInTheDocument();
  });
});
