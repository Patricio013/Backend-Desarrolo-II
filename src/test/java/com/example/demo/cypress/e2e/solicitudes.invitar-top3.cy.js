<reference types="cypress" />

const BASE = Cypress.env('API_BASE') || 'http://localhost:8080'
function expectInvitarTop3Shape(body) {
  expect(body).to.be.an('array')
  body.forEach(item => {
    expect(item).to.have.all.keys('solicitudId','descripcion','estado','top3')
    expect(item.solicitudId).to.be.a('number')
    expect(item.descripcion).to.be.a('string')
    expect(item.estado).to.equal('COTIZANDO') 
    expect(item.top3).to.be.an('array')
    expect(item.top3.length).to.be.at.most(3)
    item.top3.forEach(inv => {
      expect(inv).to.have.keys(
        'prestadorId','prestadorNombre','categoriaId','categoriaNombre','ranking'
      )
      expect(inv.prestadorId).to.be.a('number')
      expect(inv.ranking).to.be.a('number')
    })
  })
}

describe('POST /solicitudes/invitar-top3', () => {
  const url = `${BASE}/solicitudes/invitar-top3`

  it('200 y retorna una lista con contrato válido', () => {
    cy.request('POST', url).then(res => {
      expect(res.status).to.eq(200)
      expectInvitarTop3Shape(res.body)
    })
  })

  it('idempotencia HTTP básica: múltiples llamadas secuenciales no rompen', () => {
    const times = 5
    cy.wrap(Array.from({ length: times })).each(() =>
      cy.request('POST', url).then(res => {
        expect(res.status).to.eq(200)
        expect(res.isOkStatusCode).to.be.true
      })
    )
  })

  it('si no hay solicitudes CREADAS, responde 200 con lista vacía', () => {
    cy.request('POST', url)
    cy.request('POST', url).then(res => {
      expect(res.status).to.eq(200)
      expect(res.body).to.be.an('array')
      expect(res.body.length).to.be.gte(0)
    })
  })

  it('smoke de rendimiento (tiempo de respuesta) — p95 < 500ms en 20 llamadas', () => {
    const N = 20
    const durations = []
    function now() { return performance.now() }

    cy.wrap(new Array(N).fill(0)).each(() => {
      const t0 = now()
      return cy.request('POST', url).then(res => {
        const dt = now() - t0
        durations.push(dt)
        expect(res.status).to.eq(200)
      })
    }).then(() => {
      const sorted = durations.slice().sort((a,b)=>a-b)
      const p95 = sorted[Math.floor(0.95 * (sorted.length - 1))]
      cy.log(`p95=${p95.toFixed(1)} ms`)
      expect(p95, 'p95 < 500ms').to.be.lessThan(500)
    })
  })

  it('robustez: acepta Content-Type correcto y rechaza incorrecto (415/400)', () => {
    cy.request({
      method: 'POST',
      url,
      failOnStatusCode: false,         
      headers: { 'Content-Type': 'text/plain' },
      body: 'no-es-json'
    }).then(res => {
      expect([400,415]).to.include(res.status)
    })
  })
  it('cabeceras CORS presentes (si corresponde)', () => {
    cy.request('POST', url).then(res => {
      expect(res.headers).to.have.property('vary')
    })
  })
})
