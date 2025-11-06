/// <reference types="cypress" />

describe('Webhook Receiver API', () => {
  const API_BASE = Cypress.env('API_BASE');

  it('should receive a generic webhook and store it', () => {
    const webhookBody = {
      topic: 'test.topic.event',
      eventName: 'testEvent',
      msgId: `msg-${Date.now()}`,
      payload: {
        data: 'some value',
        timestamp: new Date().toISOString()
      }
    };

    cy.request({
      method: 'POST',
      url: `${API_BASE}/api/webhook`,
      body: webhookBody,
      headers: { 'Content-Type': 'application/json' }
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body.module).to.eq('webhooks');
      expect(response.body.payload).to.have.property('storedEventId');
    });
  });

  it('should process a "solicitud.creada" webhook event', () => {
    const webhookBody = {
      topic: "search.solicitud.creada",
      eventName: "solicitud.creada",
      cuerpo: {
        solicitudId: 999,
        descripcion: "Test de webhook para crear solicitud",
        rubroId: 1
      }
    };

    cy.request('POST', `${API_BASE}/api/webhook`, webhookBody)
      .its('body.payload.solicitudCreada').should('be.true');
  });
});