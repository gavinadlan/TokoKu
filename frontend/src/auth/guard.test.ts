import { decideAccess } from './guard';

const user = { id: 1, email: 'a@b.c', name: 'A', role: 'USER' as const };
const admin = { ...user, role: 'ADMIN' as const };

describe('decideAccess', () => {
  it('menunggu selama sesi dipulihkan', () => {
    expect(decideAccess('loading', null)).toBe('wait');
    expect(decideAccess('loading', null, 'ADMIN')).toBe('wait');
  });
  it('anonim diarahkan masuk', () => {
    expect(decideAccess('anonymous', null)).toBe('login');
    expect(decideAccess('anonymous', null, 'ADMIN')).toBe('login');
  });
  it('user biasa boleh rute login tetapi tidak rute admin', () => {
    expect(decideAccess('authenticated', user)).toBe('allow');
    expect(decideAccess('authenticated', user, 'ADMIN')).toBe('forbidden');
  });
  it('admin boleh semuanya', () => {
    expect(decideAccess('authenticated', admin, 'ADMIN')).toBe('allow');
    expect(decideAccess('authenticated', admin)).toBe('allow');
  });
});
