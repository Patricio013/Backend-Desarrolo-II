const BASE = Cypress.env("API_BASE") || "http://localhost:8080";

describe("E2E API Tests - Solicitudes", () => {

    it("POST /api/solicitudes/invitar-top3", () => {
        cy.request("POST", `${BASE}/api/solicitudes/invitar-top3`, {})
            .then((res) => {
                expect(res.status).to.eq(200);
                expect(res.body).to.be.an("array");
            });
    });

    it("POST /api/solicitudes/crear", () => {
        cy.request("POST", `${BASE}/api/solicitudes/crear`, [
            { descripcion: "Nueva solicitud de prueba", rubro: "Electricista" }
        ]).then((res) => {
            expect(res.status).to.eq(200);
            expect(res.body[0]).to.have.property("id");
        });
    });

    it("PATCH /api/solicitudes/{id}/cancelar", () => {
        cy.request("PATCH", `${BASE}/api/solicitudes/1/cancelar`).then((res) => {
            expect(res.status).to.eq(200);
        });
    });

    it("PUT /api/solicitudes/path/{id}/recotizar", () => {
        cy.request("PUT", `${BASE}/api/solicitudes/path/1/recotizar`).then((res) => {
            expect(res.status).to.eq(200);
        });
    });

    it("POST /api/solicitudes/recibirCotizacion", () => {
        cy.request("POST", `${BASE}/api/solicitudes/recibirCotizacion`, {
            solicitudId: 1,
            prestadorId: 2,
            monto: 5000.00
        }).then((res) => {
            expect(res.status).to.eq(201);
            expect(res.body).to.have.property("solicitudID", 1);
            expect(res.body).to.have.property("prestadorID", 2);
            expect(res.body).to.have.property("monto");
        });
    });

    it("POST /api/solicitudes/asignar", () => {
        cy.request("POST", `${BASE}/api/solicitudes/asignar`, {
            solicitudId: 1,
            prestadorId: 2
        }).then((res) => {
            expect(res.status).to.eq(201);
            expect(res.body).to.have.property("pagoId");   // según tu DTO
        });
    });

    it("GET /api/solicitudes/ws", () => {
        cy.request("GET", `${BASE}/api/solicitudes/ws`).then((res) => {
            expect(res.status).to.eq(200);
            expect(res.body).to.be.an("array");
        });
    });
});