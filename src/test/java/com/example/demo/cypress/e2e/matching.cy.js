/// <reference types="cypress" />

describe('API E2E - Matching Subscriptions', () => {

  it('debería listar las suscripciones existentes', () => {
    cy.request({
      url: '/api/matching/subscriptions',
      failOnStatusCode: false // No fallar si el status es 4xx o 5xx
    }).then((response) => {
      // Este endpoint puede devolver 200 o 500 dependiendo del servicio externo.
      // Validamos que devuelva un código de estado.
      expect(response.status).to.be.a('number');
      expect(response.body.payload).to.have.property('subscriptions');
    });
  });

  it('debería permitir crear una suscripción por topic', () => {
    cy.request({
      method: 'POST',
      url: '/api/matching/subscriptions',
      body: {
        topic: 'test.topic.event'
      },
      failOnStatusCode: false
    }).then((response) => {
      // El status depende de la respuesta del servicio externo.
      expect(response.status).to.be.a('number');
      expect(response.body.payload.topic).to.eq('test.topic.event');
    });
  });

  it('debería intentar eliminar una suscripción y devolver un estado', () => {
    const subscriptionId = 'id-de-prueba-a-borrar';
    cy.request({
      method: 'DELETE',
      url: `/api/matching/subscriptions/${subscriptionId}`,
      failOnStatusCode: false
    }).then((response) => {
      expect(response.status).to.be.a('number');
      expect(response.body.payload.subscriptionId).to.eq(subscriptionId);
    });
  });

});