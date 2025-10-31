/**
 * Creates a mock JWT token with a specific role.
 * This is a client-side helper to generate a token for our tests.
 * @param {string} role - The role to include in the token payload (e.g., 'ADMIN', 'USER').
 * @returns {string} A mock JWT string.
 */
const createMockJwt = (role) => {
  const header = { alg: 'HS256', typ: 'JWT' };
  const payload = { sub: '12345', name: 'Test User', roles: [role] };

  // Base64Url encode header and payload
  const encodedHeader = btoa(JSON.stringify(header))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  const encodedPayload = btoa(JSON.stringify(payload))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');

  // A fake signature is fine for this test
  return `${encodedHeader}.${encodedPayload}.fakesignature`;
};

describe('Auth Controller E2E Tests', () => {
  const loginUrl = '/api/auth/login';
  const externalUsersServiceUrl = '**/api/users/login'; // Using wildcard to match any base URL

  const loginCredentials = {
    email: 'admin@example.com',
    password: 'password123',
  };

  context('Successful Logins', () => {
    it('should return 200 OK and a token for a successful admin login', () => {
      const adminJwt = createMockJwt('ADMIN');

      // Intercept the backend's call to the external users service
      // and return a successful response with an admin token.
      cy.intercept('POST', externalUsersServiceUrl, {
        statusCode: 200,
        body: {
          access_token: adminJwt,
        },
      }).as('externalLogin');

      // Make the login request to our backend
      cy.request('POST', loginUrl, loginCredentials).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body).to.have.property('access_token', adminJwt);
      });
    });

    it('should return 403 Forbidden for a successful login by a non-admin user', () => {
      const userJwt = createMockJwt('USER');

      // Intercept and mock the response with a non-admin token.
      cy.intercept('POST', externalUsersServiceUrl, {
        statusCode: 200,
        body: {
          token: userJwt,
        },
      }).as('externalLogin');

      // Make the login request, but expect it to fail with a 403
      cy.request({
        method: 'POST',
        url: loginUrl,
        body: loginCredentials,
        failOnStatusCode: false, // Prevent Cypress from failing the test on a 4xx/5xx response
      }).then((response) => {
        expect(response.status).to.eq(403);
        expect(response.body).to.deep.equal({ error: 'forbidden: admin only' });
      });
    });
  });

  context('Failed Logins', () => {
    it('should return 401 Unauthorized when the external service rejects credentials', () => {
      // Intercept and mock a 401 response from the external service.
      cy.intercept('POST', externalUsersServiceUrl, {
        statusCode: 401,
        body: {
          error: 'Invalid credentials',
        },
      }).as('externalLogin');

      cy.request({
        method: 'POST',
        url: loginUrl,
        body: { email: 'wrong@user.com', password: 'wrong' },
        failOnStatusCode: false,
      }).then((response) => {
        expect(response.status).to.eq(401);
        expect(response.body).to.deep.equal({ error: 'Invalid credentials' });
      });
    });

    it('should return 502 Bad Gateway when the external users service is unavailable', () => {
      // Intercept and simulate a network failure.
      cy.intercept('POST', externalUsersServiceUrl, {
        forceNetworkError: true,
      }).as('externalLogin');

      cy.request({
        method: 'POST',
        url: loginUrl,
        body: loginCredentials,
        failOnStatusCode: false,
      }).then((response) => {
        expect(response.status).to.eq(502);
        expect(response.body).to.deep.equal({ error: 'users service unavailable' });
      });
    });
  });
});