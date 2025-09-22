/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'

describe('CalificacionController', () => {
    const url = `${BASE}/prestadores/calificaciones`

    it('envia batch de calificaciones', () => {
    cy.request('POST', '/prestadores/calificaciones', [
        { id: 1, calificacion: 5 },
        { id: 2, calificacion: 3 }
    ]).then((res) => {
        expect(res.status).to.eq(200)
        expect(res.body).to.eq("ok")
    })
    })

    it('rechaza Content-Type incorrecto', () => {
        cy.request({
            method: 'POST',
            url,
            headers: { 'Content-Type': 'text/plain' },
            body: 'texto',
            failOnStatusCode: false
        }).then(res => {
            expect([400,415]).to.include(res.status)
        })
    })
})
