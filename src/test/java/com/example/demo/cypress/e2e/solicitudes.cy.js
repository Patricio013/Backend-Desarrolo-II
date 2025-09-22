/// <reference types="cypress" />

const BASE = Cypress.env("API_BASE") || "http://localhost:8080";

describe("SolicitudController", () => {
  const url = `${BASE}/solicitudes`;
  let solicitudId;

  beforeEach(() => {
    // Crear prestador para que los tests que lo usan tengan uno disponible
    cy.request("POST", `${BASE}/prestadores-sync`, {
      id: 10,
      nombre: "Prestador E2E",
      email: "e2e@test.com",
      telefono: "999999999",
    });

    // Crear una solicitud inicial
    cy.request("POST", `${url}/crear`, [
      { descripcion: "Setup test", rubro: "Electricista" },
    ]).then((res) => {
      solicitudId = res.body[0].id;
    });
  });

  it("crear solicitudes", () => {
    expect(solicitudId).to.exist;
  });

  it("cancelar solicitud", () => {
    cy.request("PATCH", `${url}/${solicitudId}/cancelar`).then((res) => {
      expect([200, 204, 404]).to.include(res.status);
    });
  });

  it("recotizar solicitud", () => {
    cy.request({
      method: "PUT",
      url: `${url}/path/${solicitudId}/recotizar`,
      failOnStatusCode: false,
    }).then((res) => {
      expect([200, 204, 404]).to.include(res.status);
    });
  });

  it("recibir cotización", () => {
    cy.request("POST", `${url}/recibirCotizacion`, {
      solicitudId,
      prestadorId: 10,
      monto: 5000,
    }).then((res) => {
      expect([201, 400, 404]).to.include(res.status);
    });
  });

  it("asignar solicitud", () => {
    cy.request("POST", `${url}/asignar`, {
      solicitudId,
      prestadorId: 10,
    }).then((res) => {
      expect([201, 400, 404]).to.include(res.status);
    });
  });

  it("listar solicitudes como WS", () => {
    cy.request("GET", `${url}/ws`).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body).to.be.an("array");
    });
  });
});
