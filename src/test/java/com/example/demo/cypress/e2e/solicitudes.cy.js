/// <reference types="cypress" />

describe('API E2E - Solicitudes', () => {
  let nuevaSolicitudId;

  it('debería poder crear una nueva solicitud y obtener un ID', () => {
    // Asumimos que para crear una solicitud se necesita un cliente, un rubro y una descripción.
    // Estos datos deberían ser realistas para que el test pase.
    const body = {
      idCliente: 1,
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
      headers: {
        'Content-Type': 'application/json'
      }
    }).then((response) => {
      expect(response.status).to.eq(200); // Simplificado para validar solo 200 OK
      expect(response.body).to.have.property('id'); // Verificamos que la respuesta devuelva un ID.
      nuevaSolicitudId = response.body.id; // Guardamos el ID para usarlo en el siguiente test.
    });
  });

  it('debería poder obtener la solicitud creada anteriormente', () => {
    // Usamos el ID de la solicitud creada en el test anterior.
    // Esto demuestra cómo encadenar tests para simular un flujo real.
    cy.request({
      method: 'GET',
      url: `/api/solicitudes/${nuevaSolicitudId}`
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body.id).to.eq(nuevaSolicitudId);
      expect(response.body.descripcion).to.eq('Tengo una pérdida de agua en el baño principal.');
    });
  });

  it('debería invitar al top 3 de proveedores para una solicitud existente', () => {
    // Este test ahora usa el ID de la solicitud que creamos.
    cy.request({
      method: 'POST',
      url: '/api/solicitudes/invitar-top3',
      body: {
        idSolicitud: nuevaSolicitudId
      },
      headers: {
        'Content-Type': 'application/json'
      }
    }).then((response) => {
      expect(response.status).to.eq(200);
      // Opcional: verificar que el cuerpo de la respuesta sea el esperado.
      expect(response.body.message).to.contain('Invitaciones enviadas');
    });
  });

});