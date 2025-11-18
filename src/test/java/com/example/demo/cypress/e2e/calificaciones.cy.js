/// <reference types="cypress" />

describe('API E2E - Calificaciones', () => {

  const testData = {};

  beforeEach(() => {
    // Creamos los prestadores necesarios antes de cada test para no depender de una BD poblada.
    const prestador1 = { id: 999991, nombre: 'Juan', apellido: 'E2E', email: 'juan.e2e@test.com', rubros: [1] };
    const prestador2 = { id: 999992, nombre: 'Ana', apellido: 'E2E', email: 'ana.e2e@test.com', rubros: [2] };

    cy.request('POST', '/api/prestadores-sync', prestador1).then(response => {
      expect(response.status).to.eq(200);
      testData.prestador1Id = prestador1.id;
    });

    cy.request('POST', '/api/prestadores-sync', prestador2).then(response => {
      expect(response.status).to.eq(200);
      testData.prestador2Id = prestador2.id;
    });
  });

  it('debería procesar un batch de calificaciones válido y devolver 200 OK', () => {
    // Nos aseguramos de que los datos del hook beforeEach estén listos.
    cy.wrap(testData).should('have.property', 'prestador1Id');
    cy.wrap(testData).should('have.property', 'prestador2Id');

    cy.then(() => {
      const calificaciones = [
        {
          id: testData.prestador1Id, // Usamos el ID del prestador creado dinámicamente
          puntuaciones: [5, 5, 4],
          comentario: "Excelente servicio"
        },
        {
          id: testData.prestador2Id,
          puntuaciones: [3, 4]
        }
      ];

      cy.request({
        method: 'POST',
        url: '/api/prestadores/calificaciones',
        body: calificaciones
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body.payload).to.eq('ok');
      });
    });
  });

  it('debería procesar un batch con items inválidos y aun así devolver 200 OK', () => {
    // El controlador está diseñado para capturar errores por ítem y continuar.
    const calificacionesMixtas = [
      { id: testData.prestador1Id, puntuaciones: [5] }, // Válido
      { /* Inválido, sin ID de prestador */ puntuaciones: [1] },
      { id: 3, /* Inválido, sin puntuaciones */ }
    ];

    cy.request({
      method: 'POST',
      url: '/api/prestadores/calificaciones',
      body: calificacionesMixtas
    }).then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body.payload).to.eq('ok');
    });
  });
});