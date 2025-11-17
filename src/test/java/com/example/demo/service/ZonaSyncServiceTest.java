package com.example.demo.service;

import com.example.demo.dto.ZonaAltaWebhookDTO;
import com.example.demo.dto.ZonaModificacionWebhookDTO;
import com.example.demo.entity.Zona;
import com.example.demo.repository.ZonaRepository;
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

class ZonaSyncServiceTest {

    @Mock
    private ZonaRepository zonaRepository;

    @InjectMocks
    private ZonaSyncService zonaSyncService;

    private ZonaAltaWebhookDTO altaDto;
    private ZonaModificacionWebhookDTO modificacionDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        altaDto = new ZonaAltaWebhookDTO();
        altaDto.setId(100L);
        altaDto.setNombre("Palermo");

        modificacionDto = new ZonaModificacionWebhookDTO();
        modificacionDto.setId(100L);
        modificacionDto.setNombre("Palermo Hollywood");
    }

    // --- Tests for upsertDesdeDTO ---

    @Test
    @DisplayName("upsertDesdeDTO - Debe crear una zona nueva si no existe")
    void testUpsert_CreaNueva() {
        when(zonaRepository.findByExternalId(altaDto.getId())).thenReturn(Optional.empty());
        when(zonaRepository.save(any(Zona.class))).thenAnswer(inv -> inv.getArgument(0));

        Zona result = zonaSyncService.upsertDesdeDTO(altaDto);

        assertNotNull(result);
        assertEquals(altaDto.getNombre(), result.getNombre());
        assertEquals(altaDto.getId(), result.getExternalId());
        verify(zonaRepository, times(1)).save(any(Zona.class));
    }

    @Test
    @DisplayName("upsertDesdeDTO - Debe actualizar una zona existente")
    void testUpsert_ActualizaExistente() {
        Zona zonaExistente = new Zona();
        zonaExistente.setId(1L);
        zonaExistente.setExternalId(altaDto.getId());
        zonaExistente.setNombre("Nombre Viejo");

        when(zonaRepository.findByExternalId(altaDto.getId())).thenReturn(Optional.of(zonaExistente));
        when(zonaRepository.save(any(Zona.class))).thenAnswer(inv -> inv.getArgument(0));

        Zona result = zonaSyncService.upsertDesdeDTO(altaDto);

        assertNotNull(result);
        assertEquals(zonaExistente.getId(), result.getId()); // Internal ID should be preserved
        assertEquals(altaDto.getNombre(), result.getNombre());
        verify(zonaRepository, times(1)).save(zonaExistente);
    }

    @Test
    @DisplayName("upsertDesdeDTO - Debe lanzar excepción si el DTO es nulo")
    void testUpsert_DtoNulo() {
        assertThrows(IllegalArgumentException.class, () -> zonaSyncService.upsertDesdeDTO((ZonaAltaWebhookDTO) null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("upsertDesdeDTO - Debe lanzar excepción si el nombre es nulo o vacío")
    void testUpsert_NombreInvalido(String nombre) {
        altaDto.setNombre(nombre);
        assertThrows(IllegalArgumentException.class, () -> zonaSyncService.upsertDesdeDTO(altaDto));
    }

    // --- Tests for upsertDesdeDTOs (batch) ---

    @Test
    @DisplayName("upsertDesdeDTOs - Debe procesar una lista y omitir los inválidos")
    void testUpsertBatch_ProcesaValidosOmitiendoInvalidos() {
        ZonaAltaWebhookDTO dtoValido = new ZonaAltaWebhookDTO();
        dtoValido.setId(101L);
        dtoValido.setNombre("Caballito");

        ZonaAltaWebhookDTO dtoInvalido = new ZonaAltaWebhookDTO();
        dtoInvalido.setId(null); // ID nulo

        when(zonaRepository.findByExternalId(dtoValido.getId())).thenReturn(Optional.empty());
        when(zonaRepository.save(any(Zona.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Zona> resultados = zonaSyncService.upsertDesdeDTOs(List.of(dtoValido, dtoInvalido));

        assertEquals(1, resultados.size());
        assertEquals(dtoValido.getId(), resultados.get(0).getExternalId());
        verify(zonaRepository, times(1)).save(any(Zona.class));
    }

    @Test
    @DisplayName("upsertDesdeDTOs - Debe devolver lista vacía si la entrada es nula o vacía")
    void testUpsertBatch_EntradaNulaOVacia() {
        assertTrue(zonaSyncService.upsertDesdeDTOs(null).isEmpty());
        assertTrue(zonaSyncService.upsertDesdeDTOs(Collections.emptyList()).isEmpty());
        verify(zonaRepository, never()).save(any());
    }

    // --- Tests for actualizarDesdeDTO ---

    @Test
    @DisplayName("actualizarDesdeDTO - Debe actualizar una zona existente correctamente")
    void testActualizar_Success() {
        Zona zonaExistente = new Zona();
        zonaExistente.setId(1L);
        zonaExistente.setExternalId(modificacionDto.getId());
        zonaExistente.setNombre("Nombre Viejo");

        when(zonaRepository.findByExternalId(modificacionDto.getId())).thenReturn(Optional.of(zonaExistente));
        when(zonaRepository.save(any(Zona.class))).thenAnswer(inv -> inv.getArgument(0));

        Zona result = zonaSyncService.actualizarDesdeDTO(modificacionDto);

        assertNotNull(result);
        assertEquals(zonaExistente.getId(), result.getId());
        assertEquals(modificacionDto.getNombre(), result.getNombre());
        verify(zonaRepository, times(1)).save(zonaExistente);
    }

    @Test
    @DisplayName("actualizarDesdeDTO - Debe lanzar excepción si la zona no existe")
    void testActualizar_ZonaNoEncontrada() {
        when(zonaRepository.findByExternalId(modificacionDto.getId())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> zonaSyncService.actualizarDesdeDTO(modificacionDto));
        assertTrue(ex.getMessage().contains("Zona no encontrada para modificación"));
        verify(zonaRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarDesdeDTO - Debe lanzar excepción si el ID es nulo")
    void testActualizar_IdNulo() {
        modificacionDto.setId(null);
        assertThrows(IllegalArgumentException.class, () -> zonaSyncService.actualizarDesdeDTO(modificacionDto));
    }

    // --- Tests for actualizarDesdeDTOs (batch) ---

    @Test
    @DisplayName("actualizarDesdeDTOs - Debe procesar una lista y omitir los no encontrados")
    void testActualizarBatch_ProcesaValidosOmitiendoNoEncontrados() {
        ZonaModificacionWebhookDTO dtoValido = new ZonaModificacionWebhookDTO();
        dtoValido.setId(101L);
        dtoValido.setNombre("Belgrano");

        ZonaModificacionWebhookDTO dtoNoEncontrado = new ZonaModificacionWebhookDTO();
        dtoNoEncontrado.setId(999L);
        dtoNoEncontrado.setNombre("Inexistente");

        Zona zonaExistente = new Zona();
        zonaExistente.setExternalId(101L);

        when(zonaRepository.findByExternalId(dtoValido.getId())).thenReturn(Optional.of(zonaExistente));
        when(zonaRepository.findByExternalId(dtoNoEncontrado.getId())).thenReturn(Optional.empty());
        when(zonaRepository.save(any(Zona.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Zona> resultados = zonaSyncService.actualizarDesdeDTOs(List.of(dtoValido, dtoNoEncontrado));

        assertEquals(1, resultados.size());
        assertEquals(dtoValido.getId(), resultados.get(0).getExternalId());
        verify(zonaRepository, times(1)).save(any(Zona.class));
    }
}