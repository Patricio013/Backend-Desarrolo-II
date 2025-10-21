describe('💳 Pagos API', () => {
  const base = '/api/pagos';

  it('crea un nuevo pago', () => {
    cy.request('POST', `${base}/crear`, {
      solicitudId: 1,
      monto: 1500,
      medioPago: 'transferencia'
    }).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body.estado).to.exist;
    });
  });

  it('actualiza un pago existente', () => {
    cy.request('PUT', `${base}/actualizar`, {
      pagoId: 1,
      estado: 'CONFIRMADO'
    }).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body.estado).to.eq('CONFIRMADO');
    });
  });

  it('retorna error al actualizar pago inexistente', () => {
    cy.request({
      method: 'PUT',
      url: `${base}/actualizar`,
      failOnStatusCode: false,
      body: { pagoId: 9999, estado: 'CONFIRMADO' }
    }).then((res) => {
      expect(res.status).to.be.oneOf([404, 400]);
    });
  });
});
