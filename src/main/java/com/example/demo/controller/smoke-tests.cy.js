/// <reference types="cypress" />

describe('Smoke Tests - API Health Checks', () => {

  it('should get a successful ping from the health check endpoint', () => {
    cy.request({
      method: 'GET',
      url: `${Cypress.env('API_BASE')}/api/test/ping`,
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body).to.have.property('module', 'health');
      expect(response.body).to.have.property('operation', 'ping');
      expect(response.body.payload).to.include('Backend funcionando OK');
    });
  });

  it('should be able to list matching subscriptions', () => {
    cy.request(`${Cypress.env('API_BASE')}/api/matching/subscriptions`)
      .its('status').should('eq', 200);
  });
});