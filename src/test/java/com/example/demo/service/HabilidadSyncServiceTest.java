package com.example.demo.service;

import com.example.demo.dto.HabilidadAltaWebhookDTO;
import com.example.demo.entity.Habilidad;
import com.example.demo.entity.Rubro;
import com.example.demo.repository.HabilidadRepository;
import com.example.demo.repository.RubroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HabilidadSyncServiceTest {

    @Mock
    private HabilidadRepository habilidadRepository;

    @Mock
    private RubroRepository rubroRepository;

    @InjectMocks
    private HabilidadSyncService habilidadSyncService;

    private HabilidadAltaWebhookDTO dto;
    private Rubro rubro;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        dto = new HabilidadAltaWebhookDTO();
        dto.setId(100L);
        dto.setNombre("Pintura Latex");
        dto.setIdRubro(200L);

        rubro = new Rubro();
        rubro.setId(1L);
        rubro.setExternalId(200L);
        rubro.setNombre("Pintureria");
    }

    // Tests for upsertDesdeDTO

    @Test
    @DisplayName("upsertDesdeDTO - Debe crear una habilidad nueva si no existe")
    void testUpsert_CreaNueva() {
        when(rubroRepository.findByExternalId(dto.getIdRubro())).thenReturn(Optional.of(rubro));
        when(habilidadRepository.findByExternalId(dto.getId())).thenReturn(Optional.empty());
        when(habilidadRepository.save(any(Habilidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Habilidad result = habilidadSyncService.upsertDesdeDTO(dto);

        assertNotNull(result);
        assertEquals(dto.getNombre(), result.getNombre());
        assertEquals(dto.getId(), result.getExternalId());
        assertEquals(rubro, result.getRubro());
        verify(habilidadRepository, times(1)).save(any(Habilidad.class));
    }

    @Test
    @DisplayName("upsertDesdeDTO - Debe actualizar una habilidad existente")
    void testUpsert_ActualizaExistente() {
        Habilidad habilidadExistente = new Habilidad();
        habilidadExistente.setId(5L);
        habilidadExistente.setExternalId(dto.getId());
        habilidadExistente.setNombre("Nombre Viejo");

        when(rubroRepository.findByExternalId(dto.getIdRubro())).thenReturn(Optional.of(rubro));
        when(habilidadRepository.findByExternalId(dto.getId())).thenReturn(Optional.of(habilidadExistente));
        when(habilidadRepository.save(any(Habilidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Habilidad result = habilidadSyncService.upsertDesdeDTO(dto);

        assertNotNull(result);
        assertEquals(habilidadExistente.getId(), result.getId()); // Internal ID should be preserved
        assertEquals(dto.getNombre(), result.getNombre());
        verify(habilidadRepository, times(1)).save(habilidadExistente);
    }

    @Test
    @DisplayName("upsertDesdeDTO - Debe lanzar excepción si el rubro no existe")
    void testUpsert_RubroNoEncontrado() {
        when(rubroRepository.findByExternalId(dto.getIdRubro())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> habilidadSyncService.upsertDesdeDTO(dto));
        assertTrue(ex.getMessage().contains("Rubro no encontrado"));
        verify(habilidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("upsertDesdeDTO - Debe lanzar excepción si el DTO es nulo")
    void testUpsert_DtoNulo() {
        assertThrows(IllegalArgumentException.class, () -> habilidadSyncService.upsertDesdeDTO(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("upsertDesdeDTO - Debe lanzar excepción si el nombre es nulo o vacío")
    void testUpsert_NombreInvalido(String nombre) {
        dto.setNombre(nombre);
        assertThrows(IllegalArgumentException.class, () -> habilidadSyncService.upsertDesdeDTO(dto));
    }

    // Tests for actualizarDesdeDTO

    @Test
    @DisplayName("actualizarDesdeDTO - Debe actualizar una habilidad existente correctamente")
    void testActualizar_Success() {
        Habilidad habilidadExistente = new Habilidad();
        habilidadExistente.setId(5L);
        habilidadExistente.setExternalId(dto.getId());
        habilidadExistente.setNombre("Nombre Viejo");

        when(rubroRepository.findByExternalId(dto.getIdRubro())).thenReturn(Optional.of(rubro));
        when(habilidadRepository.findByExternalId(dto.getId())).thenReturn(Optional.of(habilidadExistente));
        when(habilidadRepository.save(any(Habilidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Habilidad result = habilidadSyncService.actualizarDesdeDTO(dto);

        assertNotNull(result);
        assertEquals(habilidadExistente.getId(), result.getId());
        assertEquals(dto.getNombre(), result.getNombre());
        assertEquals(rubro, result.getRubro());
        verify(habilidadRepository, times(1)).save(habilidadExistente);
    }

    @Test
    @DisplayName("actualizarDesdeDTO - Debe lanzar excepción si la habilidad no existe")
    void testActualizar_HabilidadNoEncontrada() {
        when(habilidadRepository.findByExternalId(dto.getId())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> habilidadSyncService.actualizarDesdeDTO(dto));
        assertTrue(ex.getMessage().contains("Habilidad no encontrada"));
        verify(habilidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarDesdeDTO - Debe lanzar excepción si el rubro no existe")
    void testActualizar_RubroNoEncontrado() {
        Habilidad habilidadExistente = new Habilidad();
        habilidadExistente.setId(5L);

        when(habilidadRepository.findByExternalId(dto.getId())).thenReturn(Optional.of(habilidadExistente));
        when(rubroRepository.findByExternalId(dto.getIdRubro())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> habilidadSyncService.actualizarDesdeDTO(dto));
        assertTrue(ex.getMessage().contains("Rubro no encontrado"));
        verify(habilidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarDesdeDTO - Debe lanzar excepción si el DTO es nulo")
    void testActualizar_DtoNulo() {
        assertThrows(IllegalArgumentException.class, () -> habilidadSyncService.actualizarDesdeDTO(null));
    }
}