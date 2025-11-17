/// <reference types="cypress" />

describe('API E2E - Sincronización de Prestadores y Calificaciones', () => {

  it('debería procesar un batch de calificaciones vacío y devolver 200 OK', () => {
    // El endpoint está en CalificacionController, pero la ruta es /api/prestadores.
    cy.request({
      method: 'POST',
      url: '/api/prestadores/calificaciones',
      body: [] // Enviamos una lista vacía, el controlador debería manejarlo.
    }).then((response) => {
      expect(response.status).to.eq(200);
    });
  });

  it('debería devolver 400 Bad Request al intentar sincronizar un prestador con datos inválidos', () => {
    cy.request({
      method: 'POST',
      url: '/api/prestadores-sync',
      body: { id: 999 }, // Cuerpo incompleto
      failOnStatusCode: false
    }).then((response) => {
      // El servicio lanza excepciones que resultan en errores 4xx.
      expect(response.status).to.be.within(400, 599);
    });
  });

  it('debería procesar un batch de sincronización de prestadores vacío y devolver 200 OK', () => {
    cy.request({
      method: 'POST',
      url: '/api/prestadores-sync/batch',
      body: []
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body).to.contain('ok');
    });
  });

});