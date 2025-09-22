/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'

describe('CalificacionController', () => {
  const url = `${BASE}/prestadores/calificaciones`

  it('envía batch de calificaciones', () => {
    cy.request('POST', url, [
      { id: 1, calificacion: 5 },
      { id: 2, calificacion: 3 }
    ]).then((res) => {
      expect(res.status).to.eq(200)
      expect(res.body).to.eq("ok")
    })
  })
})
