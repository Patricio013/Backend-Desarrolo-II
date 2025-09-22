/// <reference types="cypress" />

const BASE = Cypress.env("API_BASE") || "http://localhost:8080";

describe("PrestadorSyncController", () => {
  const url = `${BASE}/prestadores-sync`;

  it("upsert de un prestador", () => {
    cy.request("POST", url, {
      id: 1,
      nombre: "Juan Perez",
      email: "juan@test.com",
      telefono: "123456789",
      rubro: "Electricista",
      cuit: "20-12345678-9",
      direccion: "Calle Real 456"
    }).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body).to.eq("ok");
    });
  });

  it("upsert batch prestadores", () => {
    cy.request("POST", `${url}/batch`, [
      {
        id: 1,
        nombre: "Juan",
        email: "juan@test.com",
        telefono: "123",
        rubro: "Gasista",
        cuit: "20-87654321-0",
        direccion: "Ruta 8 Km 32"
      },
      {
        id: 2,
        nombre: "Maria",
        email: "maria@test.com",
        telefono: "456",
        rubro: "Plomería",
        cuit: "27-11223344-5",
        direccion: "Boulevard Central 55"
      },
    ]).then((res) => {
      expect(res.status).to.eq(200);
      expect(res.body).to.eq("ok");
    });
  });

  it("rechaza batch inválido", () => {
    cy.request({
      method: "POST",
      url: `${url}/batch`,
      body: [{ id: 104, nombre: null }],
      failOnStatusCode: false,
    }).then((res) => {
      expect([400, 500]).to.include(res.status);
    });
  });
});
