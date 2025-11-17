/// <reference types="cypress" />

describe('API E2E - Proveedores', () => {

  it('debería obtener una lista de proveedores', () => {
    cy.request({
      method: 'GET',
      url: '/api/proveedores' // Endpoint para listar proveedores
    }).then((response) => {
      expect(response.status).to.eq(200);
      // Verificamos que la respuesta sea un array y no esté vacío.
      expect(response.body).to.be.an('array').and.not.be.empty;
    });
  });

  it('debería obtener los detalles de un proveedor específico', () => {
    // Asumimos que existe un proveedor con ID = 1.
    // En un escenario real, podrías crear un proveedor primero.
    const proveedorId = 1;

    cy.request(`/api/proveedores/${proveedorId}`).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body).to.have.property('id', proveedorId);
      expect(response.body).to.have.property('nombre');
    });
  });

});