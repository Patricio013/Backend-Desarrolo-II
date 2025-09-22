const BASE = Cypress.env("API_BASE") || "http://localhost:8080";

describe("E2E API Tests - Solicitudes", () => {

    it("POST /solicitudes/invitar-top3", () => {
        cy.request("POST", `${BASE}/solicitudes/invitar-top3`, {})
            .then((res) => {
                expect(res.status).to.eq(200);
                expect(res.body).to.be.an("array");
            });
    });

    it("POST /solicitudes/crear", () => {
        cy.request("POST", `${BASE}/solicitudes/crear`, [
            { descripcion: "Nueva solicitud de prueba", rubro: "Electricista" }
        ]).then((res) => {
            expect(res.status).to.eq(200);
            expect(res.body[0]).to.have.property("id");
        });
    });

    
    it('cancela una solicitud', () => {
    cy.request('PATCH', '/solicitudes/1/cancelar').then((res) => {
        expect(res.status).to.eq(200) // void -> 200 OK
    })
    })


    it('recotiza una solicitud', () => {
    cy.request('PUT', '/solicitudes/path/1/recotizar').then((res) => {
        expect(res.status).to.eq(200)
        expect(res.body).to.have.property('solicitudId', 1)
    })
    })

    

    it('recibe una cotizacion', () => {
    cy.request('POST', '/solicitudes/recibirCotizacion', {
        solicitudId: 1,
        prestadorId: 2,
        monto: 5000
    }).then((res) => {
        expect(res.status).to.eq(201)
        expect(res.body).to.have.property('solicitudID', 1)
    })
    })  

    it('asigna solicitud', () => {
    cy.request('POST', '/solicitudes/asignar', {
        solicitudId: 1,
        prestadorId: 2
    }).then((res) => {
        expect(res.status).to.eq(201)
        expect(res.body).to.have.property('solicitudId', 1)
    })
    })

    it("GET /solicitudes/ws", () => {
        cy.request("GET", `${BASE}/solicitudes/ws`).then((res) => {
            expect(res.status).to.eq(200);
            expect(res.body).to.be.an("array");
        });
    });
});