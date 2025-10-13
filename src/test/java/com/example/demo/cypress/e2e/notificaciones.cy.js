describe('Flujo de Notificaciones', () => {
    let notificacionId;

    before(() => {
        // Paso 1: Crear una notificación para poder probar.
        // Esto se puede hacer llamando a un endpoint que genere una notificación,
        // o insertando directamente en la base de datos si el test tiene acceso.
        // Aquí simulamos la creación a través de la asignación de una solicitud,
        // que según el código, genera una notificación de pago.

        // Primero, creamos una solicitud y una cotización para poder asignar.
        // (Estos IDs deben existir en tu entorno de prueba)
        const solicitudId = 1;
        const prestadorId = 1;
        const monto = 1500.00;

        // Asumimos que ya existe una cotización del prestador 1 para la solicitud 1
        cy.request('POST', '/api/solicitudes/asignar', {
            solicitudId: solicitudId,
            prestadorId: prestadorId,
            monto: monto,
            concepto: 'Pago de prueba para notificacion E2E'
        }).then(() => {
            // La asignación debería haber creado una notificación.
            // Ahora la buscamos.
            cy.request('/api/notificaciones/pendientes').then((response) => {
                const notificaciones = response.body;
                const miNotificacion = notificaciones.find(n => n.mensaje.includes('Pago de prueba'));
                expect(miNotificacion).to.not.be.undefined;
                notificacionId = miNotificacion.id;
            });
        });
    });

    it('debería poder marcar una notificación como leída y luego eliminarla', () => {
        // Marcar como leída
        cy.request('POST', `/api/notificaciones/${notificacionId}/leida`).its('status').should('eq', 200);

        // Eliminar la notificación
        cy.request('DELETE', `/api/notificaciones/${notificacionId}`).its('status').should('eq', 204);
    });
});