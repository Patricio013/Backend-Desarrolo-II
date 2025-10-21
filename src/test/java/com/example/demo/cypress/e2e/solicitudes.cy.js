describe('🧾 Solicitudes API', () => {
  const base = '/api/solicitudes';

  it('crea una nueva solicitud', () => {
    cy.request('POST', `${base}/crear`, {
      descripcion: 'Solicitud creada desde Cypress',
      rubroId: 1,
      usuarioId: 2,
      zonaId: 1
    }).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body).to.have.property('id');
      expect(res.body.descripcion).to.include('Cypress');
    });
  });

  it('obtiene solicitudes creadas', () => {
    cy.request('GET', `${base}/creadas/2`).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body).to.be.an('array');
    });
  });

  it('asigna un prestador a solicitud', () => {
    cy.request('PUT', `${base}/asignar`, {
      solicitudId: 1,
      prestadorId: 3
    }).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body.success).to.be.true;
    });
  });

  it('maneja solicitud inexistente', () => {
    cy.request({
      method: 'GET',
      url: `${base}/detalle/9999`,
      failOnStatusCode: false
    }).then((res) => {
      expect(res.status).to.be.oneOf([404, 400]);
    });
  });
});
