/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'

describe('PagosController', () => {
  const url = `${BASE}/pagos`

  it('crea un pago', () => {
    cy.request('POST', url, {
      solicitudId: 1,
      monto: 1000
    }).then((res) => {
      expect(res.status).to.eq(200)
      expect(res.body).to.have.property('solicitudId')
    })
  })

  it('lista pagos recientes', () => {
    cy.request('GET', `${url}/ultimas`).then((res) => {
      expect(res.status).to.eq(200)
      expect(res.body).to.be.an('array')
    })
  })

  it('obtiene un pago por id', () => {
    cy.request('POST', url, { solicitudId: 2, monto: 300 })
      .then(created => {
        return cy.request(`${url}/${created.body.solicitudId}`).then(res => {
          expect(res.status).to.eq(200)
          expect(res.body.solicitudId).to.eq(created.body.solicitudId)
        })
      })
  })
})
