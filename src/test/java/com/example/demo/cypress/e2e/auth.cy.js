/// <reference types="cypress" />

describe('API E2E - Autenticación', () => {

  it('debería devolver un error de cliente al intentar hacer login con credenciales inválidas', () => {
    // Probamos que un login con datos incorrectos es rechazado por el servicio de usuarios.
    cy.request({
      method: 'POST',
      url: '/api/auth/login',
      body: {
        username: `testuser-${Date.now()}`, // Un usuario que garantizamos que no existe
        password: 'password-incorrecto'
      },
      failOnStatusCode: false // Permitimos que el test continúe con un status 4xx
    }).then((response) => {
      // Un login fallido puede devolver 400, 401, o 403. Hacemos el test más flexible.
      expect(response.status).to.be.oneOf([400, 401, 403]);
    });
  });

  it('debería devolver 400 Bad Request al intentar hacer login sin un cuerpo de petición válido', () => {
    // Probamos la validación del DTO. Enviamos un cuerpo sin el campo 'username'.
    cy.request({
      method: 'POST',
      url: '/api/auth/login',
      body: {
        password: 'some-password' // username está ausente
      },
      failOnStatusCode: false
    }).then((response) => {
      // La anotación @Valid en el controller debería resultar en un 400 Bad Request.
      expect(response.status).to.eq(400);
    });
  });
});