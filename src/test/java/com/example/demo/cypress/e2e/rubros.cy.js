/// <reference types="cypress" />

describe('API E2E - Rubros', () => {

  it('debería obtener una lista de todos los rubros disponibles', () => {
    cy.request({
      method: 'GET',
      url: '/api/rubros' // Endpoint para listar rubros
    }).then((response) => {
      expect(response.status).to.eq(200);
    });
  });

});