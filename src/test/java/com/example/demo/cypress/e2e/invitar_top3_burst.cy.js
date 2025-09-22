/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'

describe('SolicitudController - invitarTop3 burst', () => {
  const url = `${BASE}/solicitudes/invitar-top3`

  it('invita a top3 para todas las solicitudes creadas', () => {
    cy.request('POST', url).then((res) => {
      expect(res.status).to.eq(200)
      expect(res.body).to.be.an('array')
    })
  })
})



