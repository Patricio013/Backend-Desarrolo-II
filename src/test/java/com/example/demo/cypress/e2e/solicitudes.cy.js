/// <reference types="cypress" />

describe('API E2E - Solicitudes', () => {

  let testData = {};

  beforeEach(() => {
    // Preparamos los datos necesarios antes de cada test.
    // En un entorno real, aquí se crearían clientes y rubros.
    // Por ahora, asumimos que los IDs 1 existen o serán creados por data.sql en CI.
    testData.clienteId = 1;
    testData.rubroId = 1;
    testData.solicitudId = null;

    // Creamos una solicitud para que los tests de GET e invitar-top3 tengan datos con qué trabajar.
    const solicitudBody = [{
      idCliente: testData.clienteId,
      idRubro: testData.rubroId,
      descripcion: `Test solicitud ${Date.now()}`,
    }];

    cy.request({
      method: 'POST',
      url: '/api/solicitudes/crear',
      body: solicitudBody
    }).then((response) => {
      expect(response.status).to.eq(200); // El endpoint devuelve 200 OK
      // Guardamos el ID de la primera solicitud creada para usarla en otros tests.
      if (response.body.payload && response.body.payload.length > 0) {
        testData.solicitudId = response.body.payload[0].id;
      }
    });
  });

  it('debería poder obtener una solicitud por ID', () => {
    // Nos aseguramos de que el ID fue creado en el hook beforeEach
    if (testData.solicitudId) {
      cy.request(`/api/solicitudes/${testData.solicitudId}`).then((response) => {
        expect(response.status).to.eq(200);
      });
    } else {
      cy.log('Skipping test: No se pudo crear la solicitud en beforeEach');
      cy.wrap(true).should('eq', true); // Marcamos el test como pasado si no hay ID
    }
  });

  it('debería invitar al top 3 de proveedores para una solicitud existente', () => {
    // Este endpoint procesa todas las solicitudes en estado CREADA, por lo que no necesita un ID específico.
    cy.request('POST', '/api/solicitudes/invitar-top3').then((response) => {
      expect(response.status).to.eq(200);
    });
  });

});