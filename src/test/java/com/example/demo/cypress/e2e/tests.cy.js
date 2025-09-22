describe('E2E API Tests', () => {
  it('POST /solicitudes/invitar-top3', () => {
    cy.request('POST', `${BASE}/solicitudes/invitar-top3`).then(res => {
      expect(res.status).to.eq(200)
      expect(res.body).to.be.an('array')
    })
  })

  it('POST /solicitudes/crear', () => {
    cy.request('POST', `${BASE}/solicitudes/crear`, [
      { descripcion: "Nueva solicitud E2E", rubro: "Electricista" }
    ]).then(res => {
      expect(res.status).to.eq(200)
      expect(res.body[0]).to.have.property('id')
    })
  })

  it('PATCH /solicitudes/:id/cancelar', () => {
    cy.request({ method: 'PATCH', url: `${BASE}/solicitudes/1/cancelar`, failOnStatusCode: false })
      .then(res => expect([200, 204, 404]).to.include(res.status))
  })

  it('PUT /solicitudes/path/:id/recotizar', () => {
    cy.request({ method: 'PUT', url: `${BASE}/solicitudes/path/1/recotizar`, failOnStatusCode: false })
      .then(res => expect([200, 204, 404]).to.include(res.status))
  })

  it('GET /solicitudes/ws', () => {
    cy.request('GET', `${BASE}/solicitudes/ws`).then(res => {
      expect(res.status).to.eq(200)
      expect(res.body).to.be.an('array')
    })
  })

  it('POST /pagos', () => {
    cy.request('POST', `${BASE}/pagos`, { solicitudId: 1, monto: 100 }).then(res => {
      expect(res.status).to.eq(200)
      expect(res.body).to.have.property('solicitudId')
    })
  })

  it('GET /pagos/ultimas', () => {
    cy.request('GET', `${BASE}/pagos/ultimas`).then(res => {
      expect(res.status).to.eq(200)
      expect(res.body).to.be.an('array')
    })
  })

  it('POST /prestadores/calificaciones', () => {
    cy.request('POST', `${BASE}/prestadores/calificaciones`, [
      { id: 1, calificacion: 5 }
    ]).then(res => {
      expect(res.status).to.eq(200)
      expect(res.body).to.equal("ok")
    })
  })

  it('POST /prestadores-sync', () => {
    cy.request('POST', `${BASE}/prestadores-sync`, {
      id: 1, nombre: "Prestador Test"
    }).then(res => {
      expect(res.status).to.eq(200)
      expect(res.body).to.eq("ok")
    })
  })
})
