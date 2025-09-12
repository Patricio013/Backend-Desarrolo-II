/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'

describe('PrestadorSyncController', () => {
    const url = `${BASE}/api/prestadores-sync`

    it('POST /api/prestadores-sync — upsert único devuelve 200', () => {
        const dto = { id: 101, nombre: 'Carlos Sync' }

        cy.request('POST', url, dto).then(res => {
            expect(res.status).to.eq(200)
            expect(res.body).to.equal('ok')
        })
    })

    it('POST /api/prestadores-sync/batch — inserta lista', () => {
        const dtos = [
            { id: 102, nombre: 'María Sync' },
            { id: 103, nombre: 'José Sync' }
        ]

        cy.request('POST', `${url}/batch`, dtos).then(res => {
            expect(res.status).to.eq(200)
            expect(res.body).to.equal('ok')
        })
    })

    it('rechaza batch inválido (ej: nombre null)', () => {
        const invalid = [{ id: 104, nombre: null }]

        cy.request({
            method: 'POST',
            url: `${url}/batch`,
            body: invalid,
            failOnStatusCode: false
        }).then(res => {
            // según tu service puede responder 400 o 500
            expect([400, 500]).to.include(res.status)
        })
    })
})
