/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'

describe('E2E API Tests', () => {
    it('POST /api/solicitudes/invitar-top3', () => {
        cy.request('POST', `${BASE}/api/solicitudes/invitar-top3`).then(res => {
            expect(res.status).to.eq(200)
            expect(res.body).to.be.an('array')
        })
    })

    it('POST /api/solicitudes/crear', () => {
        cy.request('POST', `${BASE}/api/solicitudes/crear`, [
            { descripcion: "Nueva solicitud E2E" }
        ]).then(res => {
            expect(res.status).to.eq(200)
        })
    })

    it('PATCH /api/solicitudes/:id/cancelar', () => {
        cy.request({ method: 'PATCH', url: `${BASE}/api/solicitudes/1/cancelar`, failOnStatusCode: false })
            .then(res => expect([200,204,404]).to.include(res.status))
    })

    it('PUT /api/solicitudes/path/:id/recotizar', () => {
        cy.request({ method: 'PUT', url: `${BASE}/api/solicitudes/path/1/recotizar`, failOnStatusCode: false })
            .then(res => expect([200,204,404]).to.include(res.status))
    })

    it('GET /api/solicitudes/ws', () => {
        cy.request('GET', `${BASE}/api/solicitudes/ws`).then(res => {
            expect(res.status).to.eq(200)
            expect(res.body).to.be.an('array')
        })
    })

    it('POST /api/pagos', () => {
        cy.request('POST', `${BASE}/api/pagos`, { monto: 100, descripcion: "Pago test" }).then(res => {
            expect(res.status).to.eq(200)
        })
    })

    it('GET /api/pagos/ultimas', () => {
        cy.request('GET', `${BASE}/api/pagos/ultimas`).then(res => {
            expect(res.status).to.eq(200)
            expect(res.body).to.be.an('array')
        })
    })

    it('POST /api/prestadores/calificaciones', () => {
        cy.request('POST', `${BASE}/api/prestadores/calificaciones`, [
            { id: 1, puntaje: 5 }
        ]).then(res => {
            expect(res.status).to.eq(200)
            expect(res.body).to.equal("ok")
        })
    })

    it('POST /api/prestadores-sync', () => {
        cy.request('POST', `${BASE}/api/prestadores-sync`, {
            id: 1, nombre: "Prestador Test"
        }).then(res => {
            expect(res.status).to.eq(200)
        })
    })
})
