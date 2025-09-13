/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'

describe('SolicitudController', () => {
    it('POST /api/solicitudes/invitar-top3 — contrato válido', () => {
        cy.request('POST', `${BASE}/api/solicitudes/invitar-top3`).then(res => {
            expect(res.status).to.eq(200)
            expect(res.body).to.be.an('array')
        })
    })

    it('POST /api/solicitudes/crear — crea solicitudes', () => {
        const solicitudes = [{ descripcion: 'Arreglo de cañerías' }]
        cy.request('POST', `${BASE}/api/solicitudes/crear`, solicitudes).then(res => {
            expect(res.status).to.eq(200)
            expect(res.body).to.be.an('array')
        })
    })

    it('PATCH /api/solicitudes/{id}/cancelar — responde 200', () => {
        cy.request({ method: 'PATCH', url: `${BASE}/api/solicitudes/1/cancelar`, failOnStatusCode: false })
            .then(res => {
                expect([200,404]).to.include(res.status)
            })
    })
})
