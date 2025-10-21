describe('🔄 Prestador Sync API', () => {
  const base = '/api/prestador-sync';

  it('sincroniza datos de prestador', () => {
    cy.request('POST', `${base}/sync`, {
      email: 'prestador@example.com',
      rubros: ['Plomería', 'Electricidad']
    }).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body.synced).to.be.true;
    });
  });

  it('devuelve error si falta email', () => {
    cy.request({
      method: 'POST',
      url: `${base}/sync`,
      body: {},
      failOnStatusCode: false
    }).then((res) => {
      expect(res.status).to.eq(400);
    });
  });
});
