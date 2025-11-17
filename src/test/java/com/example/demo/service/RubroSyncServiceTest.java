package com.example.demo.service;

import com.example.demo.dto.RubroAltaWebhookDTO;
import com.example.demo.dto.RubroModificacionWebhookDTO;
import com.example.demo.entity.Rubro;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RubroSyncServiceTest {

    @Mock
    private RubroRepository rubroRepository;

    @InjectMocks
    private RubroSyncService rubroSyncService;

    private RubroAltaWebhookDTO altaDto;
    private RubroModificacionWebhookDTO modificacionDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        altaDto = new RubroAltaWebhookDTO();
        altaDto.setId(100L);
        altaDto.setNombre("Plomería");

        modificacionDto = new RubroModificacionWebhookDTO();
        modificacionDto.setId(100L);
        modificacionDto.setNombre("Plomería y Gas");
    }

    // --- Tests for upsertDesdeDTO ---

    @Test
    @DisplayName("upsertDesdeDTO - Debe crear un rubro nuevo si no existe")
    void testUpsert_CreaNuevo() {
        when(rubroRepository.findByExternalId(altaDto.getId())).thenReturn(Optional.empty());
        when(rubroRepository.save(any(Rubro.class))).thenAnswer(inv -> inv.getArgument(0));

        Rubro result = rubroSyncService.upsertDesdeDTO(altaDto);

        assertNotNull(result);
        assertEquals(altaDto.getNombre(), result.getNombre());
        assertEquals(altaDto.getId(), result.getExternalId());
        verify(rubroRepository, times(1)).save(any(Rubro.class));
    }

    @Test
    @DisplayName("upsertDesdeDTO - Debe actualizar un rubro existente")
    void testUpsert_ActualizaExistente() {
        Rubro rubroExistente = new Rubro();
        rubroExistente.setId(1L);
        rubroExistente.setExternalId(altaDto.getId());
        rubroExistente.setNombre("Nombre Viejo");

        when(rubroRepository.findByExternalId(altaDto.getId())).thenReturn(Optional.of(rubroExistente));
        when(rubroRepository.save(any(Rubro.class))).thenAnswer(inv -> inv.getArgument(0));

        Rubro result = rubroSyncService.upsertDesdeDTO(altaDto);

        assertNotNull(result);
        assertEquals(rubroExistente.getId(), result.getId()); // Internal ID should be preserved
        assertEquals(altaDto.getNombre(), result.getNombre());
        verify(rubroRepository, times(1)).save(rubroExistente);
    }

    @Test
    @DisplayName("upsertDesdeDTO - Debe lanzar excepción si el DTO es nulo")
    void testUpsert_DtoNulo() {
        assertThrows(IllegalArgumentException.class, () -> rubroSyncService.upsertDesdeDTO((RubroAltaWebhookDTO) null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("upsertDesdeDTO - Debe lanzar excepción si el nombre es nulo o vacío")
    void testUpsert_NombreInvalido(String nombre) {
        altaDto.setNombre(nombre);
        assertThrows(IllegalArgumentException.class, () -> rubroSyncService.upsertDesdeDTO(altaDto));
    }

    // --- Tests for upsertDesdeDTOs (batch) ---

    @Test
    @DisplayName("upsertDesdeDTOs - Debe procesar una lista y omitir los inválidos")
    void testUpsertBatch_ProcesaValidosOmitiendoInvalidos() {
        RubroAltaWebhookDTO dtoValido = new RubroAltaWebhookDTO();
        dtoValido.setId(101L);
        dtoValido.setNombre("Electricidad");

        RubroAltaWebhookDTO dtoInvalido = new RubroAltaWebhookDTO();
        dtoInvalido.setId(null); // ID nulo

        when(rubroRepository.findByExternalId(dtoValido.getId())).thenReturn(Optional.empty());
        when(rubroRepository.save(any(Rubro.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Rubro> resultados = rubroSyncService.upsertDesdeDTOs(List.of(dtoValido, dtoInvalido));

        assertEquals(1, resultados.size());
        assertEquals(dtoValido.getId(), resultados.get(0).getExternalId());
        verify(rubroRepository, times(1)).save(any(Rubro.class));
    }

    @Test
    @DisplayName("upsertDesdeDTOs - Debe devolver lista vacía si la entrada es nula o vacía")
    void testUpsertBatch_EntradaNulaOVacia() {
        assertTrue(rubroSyncService.upsertDesdeDTOs(null).isEmpty());
        assertTrue(rubroSyncService.upsertDesdeDTOs(Collections.emptyList()).isEmpty());
        verify(rubroRepository, never()).save(any());
    }

    // --- Tests for actualizarDesdeDTO ---

    @Test
    @DisplayName("actualizarDesdeDTO - Debe actualizar un rubro existente correctamente")
    void testActualizar_Success() {
        Rubro rubroExistente = new Rubro();
        rubroExistente.setId(1L);
        rubroExistente.setExternalId(modificacionDto.getId());
        rubroExistente.setNombre("Nombre Viejo");

        when(rubroRepository.findByExternalId(modificacionDto.getId())).thenReturn(Optional.of(rubroExistente));
        when(rubroRepository.save(any(Rubro.class))).thenAnswer(inv -> inv.getArgument(0));

        Rubro result = rubroSyncService.actualizarDesdeDTO(modificacionDto);

        assertNotNull(result);
        assertEquals(rubroExistente.getId(), result.getId());
        assertEquals(modificacionDto.getNombre(), result.getNombre());
        verify(rubroRepository, times(1)).save(rubroExistente);
    }

    @Test
    @DisplayName("actualizarDesdeDTO - Debe lanzar excepción si el rubro no existe")
    void testActualizar_RubroNoEncontrado() {
        when(rubroRepository.findByExternalId(modificacionDto.getId())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> rubroSyncService.actualizarDesdeDTO(modificacionDto));
        assertTrue(ex.getMessage().contains("Rubro no encontrado para modificación"));
        verify(rubroRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarDesdeDTO - Debe lanzar excepción si el ID es nulo")
    void testActualizar_IdNulo() {
        modificacionDto.setId(null);
        assertThrows(IllegalArgumentException.class, () -> rubroSyncService.actualizarDesdeDTO(modificacionDto));
    }

    // --- Tests for actualizarDesdeDTOs (batch) ---

    @Test
    @DisplayName("actualizarDesdeDTOs - Debe procesar una lista y omitir los no encontrados")
    void testActualizarBatch_ProcesaValidosOmitiendoNoEncontrados() {
        RubroModificacionWebhookDTO dtoValido = new RubroModificacionWebhookDTO();
        dtoValido.setId(101L);
        dtoValido.setNombre("Electricidad");

        RubroModificacionWebhookDTO dtoNoEncontrado = new RubroModificacionWebhookDTO();
        dtoNoEncontrado.setId(999L);
        dtoNoEncontrado.setNombre("Inexistente");

        Rubro rubroExistente = new Rubro();
        rubroExistente.setExternalId(101L);

        when(rubroRepository.findByExternalId(dtoValido.getId())).thenReturn(Optional.of(rubroExistente));
        when(rubroRepository.findByExternalId(dtoNoEncontrado.getId())).thenReturn(Optional.empty());
        when(rubroRepository.save(any(Rubro.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Rubro> resultados = rubroSyncService.actualizarDesdeDTOs(List.of(dtoValido, dtoNoEncontrado));

        assertEquals(1, resultados.size());
        assertEquals(dtoValido.getId(), resultados.get(0).getExternalId());
        verify(rubroRepository, times(1)).save(any(Rubro.class));
    }
}