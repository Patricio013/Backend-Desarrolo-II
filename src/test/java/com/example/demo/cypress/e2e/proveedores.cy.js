/// <reference types="cypress" />

describe('API E2E - Proveedores', () => {

  it('debería obtener una lista de proveedores', () => {
    cy.request({
      method: 'GET',
      url: '/api/proveedores' // Endpoint para listar proveedores
    }).then((response) => {
      expect(response.status).to.eq(200);
    });
  });

  it('debería obtener los detalles de un proveedor específico', () => {
    // Usamos un ID fijo que debería existir en un entorno de prueba.
    cy.request('/api/proveedores/1').then((response) => {
      expect(response.status).to.eq(200);
    });
  });

});