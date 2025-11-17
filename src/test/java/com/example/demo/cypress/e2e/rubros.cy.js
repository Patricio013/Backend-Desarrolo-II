/// <reference types="cypress" />

describe('API E2E - Rubros', () => {

  it('debería obtener una lista de todos los rubros disponibles', () => {
    cy.request({
      method: 'GET',
      url: '/api/rubros' // Endpoint para listar rubros
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body).to.be.an('array').and.not.be.empty;
      // Verificamos que al menos un rubro tenga las propiedades esperadas.
      expect(response.body[0]).to.have.property('id');
      expect(response.body[0]).to.have.property('nombre');
    });
  });

});