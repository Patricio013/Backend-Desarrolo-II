/// <reference types="cypress" />

describe('API E2E - Sistema y Autenticación', () => {

  it('debería responder OK en el endpoint de health check', () => {
    cy.request('/api/test/ping').then((response) => {
      expect(response.status).to.eq(200);
      expect(response.body.payload).to.eq('Backend funcionando OK 🚀');
    });
  });

  it('debería devolver 400 Bad Request al intentar hacer login con credenciales inválidas', () => {
    // Un login exitoso daría 200, pero es difícil de probar sin usuarios reales.
    // En cambio, probamos que la validación de un request incorrecto funciona.
    cy.request({
      method: 'POST',
      url: '/api/auth/login',
      body: {
        username: 'usuario-inexistente',
        password: 'password-incorrecto'
      },
      failOnStatusCode: false
    }).then((response) => {
      // Esperamos un error de cliente (4xx), usualmente 400, 401 o 403.
      expect(response.status).to.be.within(400, 403);
    });
  });
});