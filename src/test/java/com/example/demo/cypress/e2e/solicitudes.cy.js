/// <reference types="cypress" />

describe('API E2E - Solicitudes', () => {

  it('debería poder crear una nueva solicitud y obtener un ID', () => {
    const body = {
      idCliente: 1, // Usamos IDs fijos que deberían existir en un entorno de prueba poblado
      idRubro: 1,
      descripcion: 'Tengo una pérdida de agua en el baño principal.',
      ubicacion: {
        latitud: -34.6037,
        longitud: -58.3816
      }
    };
    cy.request({
      method: 'POST',
      url: '/api/solicitudes', // Endpoint para crear una solicitud
      body: body,
      failOnStatusCode: false // Para que no falle si devuelve otro status
    }).then((response) => {
      // Un endpoint de creación puede devolver 201, pero lo simplificamos a 200
      expect(response.status).to.eq(200);
    });
  });

  it('debería poder obtener una solicitud por ID', () => {
    cy.request('/api/solicitudes/1').then((response) => {
      expect(response.status).to.eq(200);
    });
  });

  it('debería invitar al top 3 de proveedores para una solicitud existente', () => {
    cy.request({
      method: 'POST',
      url: '/api/solicitudes/invitar-top3',
      body: { idSolicitud: 1 }
    }).then((response) => {
      expect(response.status).to.eq(200);
    });
  });

});