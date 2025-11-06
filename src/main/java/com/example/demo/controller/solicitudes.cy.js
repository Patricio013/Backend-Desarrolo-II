/// <reference types="cypress" />

describe('Solicitudes API', () => {
  const API_BASE = Cypress.env('API_BASE');

  it('should create a new solicitud from an event DTO', () => {
    const solicitudesDto = [{
      solicitudId: 1001,
      clienteId: 2002,
      rubroId: 3003,
      descripcion: 'Mi canilla de la cocina pierde agua sin parar.',
      fechaAlta: new Date().toISOString(),
      esUrgente: true,
      direccion: 'Calle Falsa 123, Piso 4, Depto A',
      coordenadas: '-34.603722,-58.381592'
    }];

    cy.request({
      method: 'POST',
      url: `${API_BASE}/api/solicitudes/crear`,
      body: solicitudesDto,
      headers: {
        'Content-Type': 'application/json'
      }
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body.operation).to.eq('solicitudesCreadas');
      expect(response.body.payload).to.be.an('array').and.not.be.empty;
      
      const created = response.body.payload[0];
      expect(created.descripcion).to.eq(solicitudesDto[0].descripcion);
      expect(created.estado).to.eq('CREADA');

      // Guardamos el ID para usarlo en otros tests
      cy.wrap(created.id).as('solicitudId');
    });
  });

  it('should get details of a specific solicitud', function() {
    // Usamos el ID de la solicitud creada en el test anterior
    cy.get('@solicitudId').then((solicitudId) => {
      cy.request(`${API_BASE}/api/solicitudes/${solicitudId}`)
        .then((response) => {
          expect(response.status).to.eq(200);
          expect(response.body.operation).to.eq('solicitudDetalle');
          expect(response.body.payload.id).to.eq(solicitudId);
        });
    });
  });

  it('should cancel a solicitud', function() {
    cy.get('@solicitudId').then((solicitudId) => {
      cy.request({
        method: 'PATCH',
        url: `${API_BASE}/api/solicitudes/${solicitudId}/cancelar`,
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.payload.solicitudId).to.eq(solicitudId);
        expect(response.body.payload.status).to.eq('cancelled');
      });
    });
  });
});