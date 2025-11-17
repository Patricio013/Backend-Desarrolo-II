/// <reference types="cypress" />

describe('API E2E - Pagos', () => {

  it('debería devolver 400 Bad Request al intentar crear una solicitud de pago sin datos', () => {
    // Probamos el caso de error, ya que crear un pago válido requiere una cotización aceptada.
    cy.request({
      method: 'POST',
      url: '/api/pagos',
      body: {},
      failOnStatusCode: false
    }).then((response) => {
      expect(response.status).to.eq(400);
    });
  });

  it('debería obtener una lista de las últimas solicitudes de pago', () => {
    cy.request('/api/pagos/ultimas').then((response) => {
      expect(response.status).to.eq(200);
      // La respuesta debería ser un array, aunque esté vacío.
    });
  });

});