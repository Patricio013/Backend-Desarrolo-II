/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'

describe('PrestadorSyncController', () => {
  const url = `${BASE}/prestadores-sync`

  it('upsert de un prestador', () => {
    cy.request('POST', url, {
      id: 1,
      nombre: "Juan Perez"
    }).then((res) => {
      expect(res.status).to.eq(200)
      expect(res.body).to.eq("ok")
    })
  })

  it('upsert batch prestadores', () => {
    cy.request('POST', `${url}/batch`, [
      { id: 1, nombre: "Juan" },
      { id: 2, nombre: "Maria" }
    ]).then((res) => {
      expect(res.status).to.eq(200)
      expect(res.body).to.eq("ok")
    })
  })

  it('rechaza batch inválido (ej: nombre null)', () => {
    cy.request({
      method: 'POST',
      url: `${url}/batch`,
      body: [{ id: 104, nombre: null }],
      failOnStatusCode: false
    }).then(res => {
      expect([400, 500]).to.include(res.status)
    })
  })
})
