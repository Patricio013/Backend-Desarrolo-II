/// <reference types="cypress" />

describe('API E2E - Prestadores', () => {

  it('debería obtener una lista de proveedores', () => {
    cy.request({
      method: 'GET',
      url: '/api/prestadores' // Endpoint para listar prestadores
    }).then((response) => {
      expect(response.status).to.eq(200);
    });
  });


});