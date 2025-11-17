/// <reference types="cypress" />

describe('API E2E - Solicitudes', () => {

  it('should return status 200 when inviting top 3 providers', () => {
    // Este test replica la llamada del script de k6.
    // Cypress puede hacer tanto tests de UI como tests de API.
    
    cy.request({
      method: 'POST',
      url: '/api/solicitudes/invitar-top3', // Cypress usará el `baseUrl` de la configuración
      body: {
        // Asumimos un cuerpo vacío como en el script de k6.
        // Si tu endpoint requiere datos, deberías añadirlos aquí.
        // Ejemplo: idSolicitud: 123
      },
      headers: {
        'Content-Type': 'application/json'
      }
    }).then((response) => {
      // Verificamos que la respuesta del servidor sea la esperada
      expect(response.status).to.eq(200);
    });
  });

});