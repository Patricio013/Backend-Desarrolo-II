const fetch = require('node-fetch')

module.exports = {
  e2e: {
    setupNodeEvents(on, config) {
      on('task', {
        async burstPost({ url, times = 20 }) {
          const payload = { } 
          const headers = { 'Content-Type': 'application/json' }
          const jobs = Array.from({ length: times }, () =>
            fetch(url, { method: 'POST', headers, body: JSON.stringify(payload) })
              .then(async r => ({ status: r.status, text: await r.text() }))
              .catch(err => ({ error: err.message }))
          )
          const results = await Promise.all(jobs)
          return results
        }
      })
      return config
    },
    env: {
      API_BASE: 'http://localhost:8080'
    },
    supportFile: false,
    specPattern: "src/test/java/com/example/demo/cypress/e2e/**/*.cy.{js,jsx,ts,tsx}",
    reporter: 'json',
    reporterOptions: {
      output: 'out/cypress/results.json'
    }
  }
}
