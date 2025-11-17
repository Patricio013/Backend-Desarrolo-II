/// <reference types="cypress" />

describe('API E2E - Sistema y Autenticación', () => {

  it('debería responder OK en el endpoint de health check', () => {
    cy.request('/api/test/ping').then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body.payload).to.eq('Backend funcionando OK 🚀');
    });
  });

});