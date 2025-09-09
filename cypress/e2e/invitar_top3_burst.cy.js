/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'
const url = `${BASE}/api/solicitudes/invitar-top3`

describe('Burst concurrente (ligero desde Node)', () => {
  it('20 POST concurrentes: no debe haber 5xx', () => {
    cy.task('burstPost', { url, times: 20 }).then(results => {
      results.forEach((r, i) => {
        expect(r.error || null, `error en #${i}`).to.be.null
        expect(r.status, `status en #${i}`).to.be.within(200, 499) 
      })
    })
  })
})
