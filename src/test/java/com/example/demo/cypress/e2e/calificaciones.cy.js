describe('⭐ Calificaciones API', () => {
  const base = '/api/calificaciones';

  it('registra calificación', () => {
    cy.request('POST', `${base}/enviar`, {
      solicitudId: 1,
      puntuacion: 5,
      comentario: 'Excelente servicio'
    }).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body.success).to.be.true;
    });
  });

  it('obtiene calificaciones de un prestador', () => {
    cy.request('GET', `${base}/prestador/3`).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body).to.be.an('array');
    });
  });

  it('maneja solicitud inexistente', () => {
    cy.request({
      method: 'GET',
      url: `${base}/prestador/9999`,
      failOnStatusCode: false
    }).then((res) => {
      expect(res.status).to.be.oneOf([404, 400]);
    });
  });
});
