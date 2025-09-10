 package com.example.demo.service;

 import com.example.demo.entity.Notificaciones;
 import com.example.demo.repository.NotificacionesRepository;

 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.stereotype.Service;

 import java.util.List;
 import java.util.Optional;

 @Service
 public class NotificacionesService {

     @Autowired
     private NotificacionesRepository notificacionesRepository;

     // Crear una notificación
     public Notificaciones crearNotificacion(Notificaciones notificacion) {
         return notificacionesRepository.save(notificacion);
     }

     // Listar todas las notificaciones de un usuario
     //public List<Notificaciones> obtenerNotificacionesPorUsuario(Long usuarioId) {
     //    return notificacionesRepository.findByUsuarioId(usuarioId);
     //}

     public Notificaciones notificarInvitacionCotizacion(Long cotizacionId, String titulo, String mensaje) {
        Notificaciones n = Notificaciones.builder()
                .cotizacionId(cotizacionId)
                .titulo(titulo)
                .mensaje(mensaje)
                .leida(false)
                .build(); // fecha/leida se setean en @PrePersist
        return notificacionesRepository.save(n);
    }

     // Marcar como leída una notificación
     public Optional<Notificaciones> marcarComoLeida(Long id) {
         Optional<Notificaciones> notificacion = notificacionesRepository.findById(id);
         if (notificacion.isPresent()) {
             Notificaciones n = notificacion.get();
             n.setLeida(true);
             notificacionesRepository.save(n);
         }
         return notificacion;
     }

     // Eliminar notificación
     public void eliminarNotificacion(Long id) {
         notificacionesRepository.deleteById(id);
     }

     public List<Notificaciones> pendientes() {
        return notificacionesRepository.findTop100ByLeidaFalseOrderByFechaAsc();
    }
 }
