/// <reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'

describe('PagosController', () => {
    const url = `${BASE}/api/pagos`

    it('POST /api/pagos — crea un pago válido', () => {
        cy.request('POST', url, { monto: 500, descripcion: 'Servicio de electricidad' })
            .then(res => {
                expect(res.status).to.eq(200)
                expect(res.body).to.have.keys('id','monto','descripcion','fechaCreacion')
                expect(res.body.id).to.be.a('number')
            })
    })

    it('GET /api/pagos/ultimas — retorna lista', () => {
        cy.request(`${url}/ultimas`).then(res => {
            expect(res.status).to.eq(200)
            expect(res.body).to.be.an('array')
        })
    })

    it('GET /api/pagos/{id} — obtiene un pago por id', () => {
        cy.request('POST', url, { monto: 300, descripcion: 'Fontanería' })
            .then(created => {
                return cy.request(`${url}/${created.body.id}`).then(res => {
                    expect(res.status).to.eq(200)
                    expect(res.body.id).to.eq(created.body.id)
                })
            })
    })
})
