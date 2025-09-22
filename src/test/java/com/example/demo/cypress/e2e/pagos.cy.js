/// <reference types="cypress" />

const BASE = Cypress.env("API_BASE") || "http://localhost:8080";

describe("PagosController", () => {
  const url = `${BASE}/pagos`;

  it("crea un pago", () => {
    cy.request("POST", `${BASE}/solicitudes/crear`, [
      {
        descripcion: "Instalación de lámpara",
        rubro: "Electricista",
        usuarioId: 1,
        direccion: "Calle Falsa 123"
      },
    ]).then((res) => {
      const solicitudId = res.body[0].id;

      cy.request("POST", url, { solicitudId, monto: 1000 }).then((pago) => {
        expect([200, 201]).to.include(pago.status);
        expect(pago.body).to.have.property("solicitudId", solicitudId);
      });
    });
  });

  it("lista pagos recientes", () => {
    cy.request("GET", `${url}/ultimas`).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body).to.be.an("array");
    });
  });

  it("obtiene un pago por id", () => {
    cy.request("POST", `${BASE}/solicitudes/crear`, [
      {
        descripcion: "Pago prueba B",
        rubro: "Plomería",
        usuarioId: 2,
        direccion: "Avenida Siempre Viva 742"
      },
    ]).then((res) => {
      const solicitudId = res.body[0].id;

      cy.request("POST", url, { solicitudId, monto: 300 }).then((created) => {
        cy.request(`${url}/${created.body.solicitudId}`).then((res2) => {
          expect(res2.status).to.eq(200);
          expect(res2.body.solicitudId).to.eq(created.body.solicitudId);
        });
      });
    });
  });
});
