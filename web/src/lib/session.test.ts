import { decodeJwt, sessionFromTokens } from './session';
import { fakeJwt } from '@/test/utils';

describe('session', () => {
  it('reads username, role, merchant and expiry from the access token claims', () => {
    const token = fakeJwt({ sub: 'medina', role: 'MERCHANT', mid: 2, exp: 2_000_000_000, typ: 'access' });
    expect(decodeJwt(token)).toMatchObject({ sub: 'medina', mid: 2 });
    expect(sessionFromTokens({ accessToken: token, refreshToken: 'r', tokenType: 'Bearer', expiresIn: 900 })).toEqual({
      accessToken: token, refreshToken: 'r', expiresAt: 2_000_000_000_000, username: 'medina', role: 'MERCHANT', merchantId: 2,
    });
  });

  it('staff tokens carry no merchant', () => {
    const token = fakeJwt({ sub: 'ops', role: 'STAFF', exp: 2_000_000_000 });
    expect(sessionFromTokens({ accessToken: token, refreshToken: 'r', tokenType: 'Bearer', expiresIn: 900 }).merchantId).toBeNull();
  });

  it('survives a malformed token', () => {
    expect(decodeJwt('not-a-jwt')).toEqual({});
  });
});
