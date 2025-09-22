/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'

describe('E2E API Tests', () => {
  it('invita top3', () => {
    cy.request('POST', `${BASE}/solicitudes/invitar-top3`).then(res => {
      expect(res.status).to.eq(200)
    })
  })

  it('crear solicitud', () => {
    cy.request('POST', `${BASE}/solicitudes/crear`, [
      { descripcion: "Instalar aire acondicionado", rubro: "Climatización" }
    ]).then(res => {
      expect(res.status).to.eq(200)
      expect(res.body[0]).to.have.property('id')
    })
  })

  it('recibir cotización', () => {
    cy.request('POST', `${BASE}/solicitudes/recibirCotizacion`, {
      solicitudId: 1,
      prestadorId: 2,
      monto: 8000
    }).then(res => {
      expect(res.status).to.eq(201)
    })
  })

  it('asignar solicitud', () => {
    cy.request('POST', `${BASE}/solicitudes/asignar`, {
      solicitudId: 1,
      prestadorId: 2
    }).then(res => {
      expect(res.status).to.eq(201)
    })
  })

  it('crear pago', () => {
    cy.request('POST', `${BASE}/pagos`, {
      solicitudId: 1,
      monto: 500
    }).then(res => {
      expect(res.status).to.eq(200)
      expect(res.body).to.have.property('solicitudId')
    })
  })

  it('listar pagos', () => {
    cy.request('GET', `${BASE}/pagos/ultimas`).then(res => {
      expect(res.status).to.eq(200)
      expect(res.body).to.be.an('array')
    })
  })

  it('enviar calificación', () => {
    cy.request('POST', `${BASE}/prestadores/calificaciones`, [
      { id: 1, calificacion: 5 }
    ]).then(res => {
      expect(res.status).to.eq(200)
      expect(res.body).to.eq("ok")
    })
  })

  it('upsert prestador', () => {
    cy.request('POST', `${BASE}/prestadores-sync`, {
      id: 1, nombre: "Prestador Test"
    }).then(res => {
      expect(res.status).to.eq(200)
      expect(res.body).to.eq("ok")
    })
  })
})
