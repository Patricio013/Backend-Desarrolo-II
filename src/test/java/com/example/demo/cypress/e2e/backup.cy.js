describe('💾 Backup API', () => {
  const base = '/api/backup';

  it('genera backup completo', () => {
    cy.request('GET', `${base}/generate`).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body.success).to.be.true;
    });
  });

  it('restaura backup', () => {
    cy.request('POST', `${base}/restore`, {
      fileName: 'backup-latest.json'
    }).then((res) => {
      expect(res.status).to.eq(200);
    });
  });

  it('maneja error de archivo inexistente', () => {
    cy.request({
      method: 'POST',
      url: `${base}/restore`,
      failOnStatusCode: false,
      body: { fileName: 'no-existe.json' }
    }).then((res) => {
      expect(res.status).to.be.oneOf([404, 400]);
    });
  });
});
