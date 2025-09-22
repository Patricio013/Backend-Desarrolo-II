/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'

describe('SolicitudController - Invitar Top3', () => {
  const url = `${BASE}/solicitudes/invitar-top3`

  it('invita top3 solicitudes', () => {
    cy.request('POST', url).then((res) => {
      expect(res.status).to.eq(200)
      expect(res.body).to.be.an('array')
    })
  })
})
  