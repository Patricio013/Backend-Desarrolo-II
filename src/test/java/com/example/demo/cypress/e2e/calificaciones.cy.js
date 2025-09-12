/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'

describe('CalificacionController', () => {
    const url = `${BASE}/api/prestadores/calificaciones`

    it('POST /calificaciones — agrega batch válido', () => {
        const items = [{ id: 1, puntaje: 5, comentario: 'Excelente' }]
        cy.request('POST', url, items).then(res => {
            expect(res.status).to.eq(200)
            expect(res.body).to.equal('ok')
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
