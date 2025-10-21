package com.example.demo.service;

import com.example.demo.dto.ModuleResponse;
import com.example.demo.entity.Prestador;
import com.example.demo.entity.Rubro;
import com.example.demo.entity.Zona;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.RubroRepository;
import com.example.demo.repository.ZonaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para PrestadorSyncService.
 * Cubre sincronización, guardado y validaciones de datos.
 */
class PrestadorSyncServiceTest {

    @Mock
    private PrestadorRepository prestadorRepository;

    @Mock
    private RubroRepository rubroRepository;

    @Mock
    private ZonaRepository zonaRepository;

    @InjectMocks
    private PrestadorSyncService prestadorSyncService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ======================================================
    // ✅ TESTS - Sincronización desde módulo externo
    // ======================================================

    @Test
    @DisplayName("Debe sincronizar prestadores correctamente desde una lista válida")
    void testSincronizarPrestadores_Success() {
        Rubro rubro = new Rubro();
        rubro.setId(1L);
        rubro.setNombre("Plomería");

        Zona zona = new Zona();
        zona.setId(2L);
        zona.setNombre("CABA");

        Prestador p = new Prestador();
        p.setId(10L);
        p.setNombre("Juan Perez");
        p.setEmail("juan@example.com");
        p.setRubro(rubro);
        p.setZona(zona);

        when(rubroRepository.findByNombre("Plomería")).thenReturn(Optional.of(rubro));
        when(zonaRepository.findByNombre("CABA")).thenReturn(Optional.of(zona));
        when(prestadorRepository.save(any(Prestador.class))).thenReturn(p);

        List<Map<String, Object>> lista = new ArrayList<>();
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Juan Perez");
        datos.put("email", "juan@example.com");
        datos.put("rubro", "Plomería");
        datos.put("zona", "CABA");
        lista.add(datos);

        ModuleResponse response = prestadorSyncService.sincronizarPrestadores(lista);

        assertEquals("OK", response.getStatus());
        assertTrue(response.getMessage().contains("1 prestador"));
        verify(prestadorRepository, times(1)).save(any(Prestador.class));
    }

    @Test
    @DisplayName("Debe ignorar prestadores con datos incompletos")
    void testSincronizarPrestadores_DatosIncompletos() {
        List<Map<String, Object>> lista = new ArrayList<>();
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Juan Perez");
        // Falta email y zona
        lista.add(datos);

        ModuleResponse response = prestadorSyncService.sincronizarPrestadores(lista);

        assertEquals("OK", response.getStatus());
        assertTrue(response.getMessage().contains("0 prestadores"));
        verify(prestadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe manejar error al guardar prestador")
    void testSincronizarPrestadores_ErrorDB() {
        Rubro rubro = new Rubro();
        rubro.setId(1L);
        rubro.setNombre("Electricidad");

        Zona zona = new Zona();
        zona.setId(2L);
        zona.setNombre("Norte");

        when(rubroRepository.findByNombre("Electricidad")).thenReturn(Optional.of(rubro));
        when(zonaRepository.findByNombre("Norte")).thenReturn(Optional.of(zona));
        when(prestadorRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        List<Map<String, Object>> lista = new ArrayList<>();
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Pedro");
        datos.put("email", "pedro@example.com");
        datos.put("rubro", "Electricidad");
        datos.put("zona", "Norte");
        lista.add(datos);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                prestadorSyncService.sincronizarPrestadores(lista));

        assertTrue(ex.getMessage().contains("DB error"));
    }

    @Test
    @DisplayName("Debe retornar respuesta vacía si la lista es nula o vacía")
    void testSincronizarPrestadores_Vacia() {
        ModuleResponse resp1 = prestadorSyncService.sincronizarPrestadores(null);
        ModuleResponse resp2 = prestadorSyncService.sincronizarPrestadores(Collections.emptyList());

        assertEquals("OK", resp1.getStatus());
        assertTrue(resp1.getMessage().contains("0 prestadores"));
        assertEquals("OK", resp2.getStatus());
        assertTrue(resp2.getMessage().contains("0 prestadores"));
    }

    // ======================================================
    // ✅ TESTS - Buscar prestador por email
    // ======================================================

    @Test
    @DisplayName("Debe obtener prestador existente por email")
    void testBuscarPrestadorPorEmail_Existe() {
        Prestador p = new Prestador();
        p.setId(1L);
        p.setEmail("test@example.com");

        when(prestadorRepository.findByEmail("test@example.com")).thenReturn(Optional.of(p));

        Prestador result = prestadorSyncService.buscarPrestadorPorEmail("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    @DisplayName("Debe retornar null si el prestador no existe")
    void testBuscarPrestadorPorEmail_NoExiste() {
        when(prestadorRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        Prestador result = prestadorSyncService.buscarPrestadorPorEmail("missing@example.com");

        assertNull(result);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el email es nulo o vacío")
    void testBuscarPrestadorPorEmail_EmailInvalido() {
        assertThrows(IllegalArgumentException.class, () ->
                prestadorSyncService.buscarPrestadorPorEmail(null));

        assertThrows(IllegalArgumentException.class, () ->
                prestadorSyncService.buscarPrestadorPorEmail(""));
    }

    // ======================================================
    // ✅ TESTS - Guardado individual
    // ======================================================

    @Test
    @DisplayName("Debe guardar prestador correctamente")
    void testGuardarPrestador_Success() {
        Prestador prestador = new Prestador();
        prestador.setId(1L);
        prestador.setEmail("juan@example.com");

        when(prestadorRepository.save(any())).thenReturn(prestador);

        Prestador result = prestadorSyncService.guardarPrestador(prestador);

        assertNotNull(result);
        assertEquals("juan@example.com", result.getEmail());
        verify(prestadorRepository, times(1)).save(prestador);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el prestador es nulo")
    void testGuardarPrestador_Nulo() {
        assertThrows(IllegalArgumentException.class, () ->
                prestadorSyncService.guardarPrestador(null));
    }

    @Test
    @DisplayName("Debe manejar error de base de datos al guardar prestador")
    void testGuardarPrestador_ErrorDB() {
        Prestador prestador = new Prestador();
        prestador.setEmail("fail@example.com");

        when(prestadorRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                prestadorSyncService.guardarPrestador(prestador));

        assertTrue(ex.getMessage().contains("DB error"));
    }
}
