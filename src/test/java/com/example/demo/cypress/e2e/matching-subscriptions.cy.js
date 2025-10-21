describe('🔔 Matching Subscription API', () => {
  const base = '/api/subscriptions';

  it('Crea una suscripción', () => {
    cy.request('POST', `${base}/subscribe`, {
      topic: 'notifications',
      event: 'new-request',
    }).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body.success).to.be.true;
    });
  });

  it('Lista suscripciones activas', () => {
    cy.request('GET', base).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body.subscriptions).to.be.an('array');
    });
  });

  it('Desuscribe correctamente', () => {
    cy.request('DELETE', `${base}/abc123`).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body.success).to.be.true;
    });
  });
});
