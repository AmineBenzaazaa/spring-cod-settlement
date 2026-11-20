import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';
import { sessionStore } from '@/lib/session';

afterEach(() => {
  cleanup();
  sessionStore.set(null);
  sessionStorage.clear();
  localStorage.clear();
});

// Recharts' ResponsiveContainer needs ResizeObserver, which jsdom does not ship.
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
(globalThis as unknown as { ResizeObserver: typeof ResizeObserverStub }).ResizeObserver ??= ResizeObserverStub;
