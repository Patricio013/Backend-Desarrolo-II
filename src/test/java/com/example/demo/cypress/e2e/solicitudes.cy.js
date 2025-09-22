/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'

describe('SolicitudController', () => {
  const url = `${BASE}/solicitudes`

  it('crear solicitudes', () => {
    cy.request('POST', `${url}/crear`, [
      { descripcion: "Cambio de lámpara", rubro: "Electricista" }
    ]).then((res) => {
      expect(res.status).to.eq(200)
      expect(res.body[0]).to.have.property('id')
    })
  })

  it('cancelar solicitud', () => {
    cy.request({
      method: 'PATCH',
      url: `${url}/1/cancelar`,
      failOnStatusCode: false
    }).then(res => {
      expect([200, 204, 404]).to.include(res.status)
    })
  })

  it('recotizar solicitud', () => {
    cy.request({
      method: 'PUT',
      url: `${url}/path/1/recotizar`,
      failOnStatusCode: false
    }).then(res => {
      expect([200, 204, 404]).to.include(res.status)
    })
  })

  it('recibir cotización', () => {
    cy.request('POST', `${url}/recibirCotizacion`, {
      solicitudId: 1,
      prestadorId: 2,
      monto: 5000
    }).then((res) => {
      expect(res.status).to.eq(201)
      expect(res.body).to.have.property('solicitudID', 1)
    })
  })

  it('asignar solicitud', () => {
    cy.request('POST', `${url}/asignar`, {
      solicitudId: 1,
      prestadorId: 2
    }).then((res) => {
      expect(res.status).to.eq(201)
      expect(res.body).to.have.property('solicitudId')
    })
  })

  it('listar solicitudes como WS', () => {
    cy.request('GET', `${url}/ws`).then((res) => {
      expect(res.status).to.eq(200)
      expect(res.body).to.be.an('array')
    })
  })
})
