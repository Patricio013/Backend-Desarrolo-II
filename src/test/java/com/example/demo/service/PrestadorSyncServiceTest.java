package com.example.demo.service;

import com.example.demo.dto.PrestadorDTO;
import com.example.demo.entity.Prestador;
import com.example.demo.entity.Zona;
import com.example.demo.repository.PrestadorRepository;
import com.example.demo.repository.HabilidadRepository;
import com.example.demo.repository.RubroRepository;
import com.example.demo.repository.ZonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * ✅ Test unitario para PrestadorSyncService
 * Mockea RabbitMQ y MatchingPublisherService
 */
class PrestadorSyncServiceTest {

    @Mock
    private PrestadorRepository prestadorRepository;

    @Mock
    private HabilidadRepository habilidadRepository;

    @Mock
    private RubroRepository rubroRepository;

    @Mock
    private ZonaRepository zonaRepository;

    @Mock
    private MatchingPublisherService matchingPublisherService;

    @InjectMocks
    private PrestadorSyncService prestadorSyncService;

    private PrestadorDTO prestadorDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        prestadorDto = new PrestadorDTO();
        prestadorDto.setId(1L);
        prestadorDto.setNombre("Juan");
        prestadorDto.setApellido("Perez");
        prestadorDto.setEmail("juan.perez@example.com");
        prestadorDto.setEstado("ACTIVO");
    }

    @Test
    @DisplayName("Debe crear un prestador nuevo si no existe")
    void testUpsertDesdeDTO_CreaNuevo() {
        when(prestadorRepository.findByExternalId(prestadorDto.getId())).thenReturn(Optional.empty());
        when(prestadorRepository.save(any(Prestador.class))).thenAnswer(i -> i.getArgument(0));

        prestadorSyncService.upsertDesdeDTO(prestadorDto);

        verify(prestadorRepository, times(1)).findByExternalId(prestadorDto.getId());
        verify(prestadorRepository, times(1)).save(any(Prestador.class));
        verify(matchingPublisherService, times(1)).publishPrestadorUpsert(any(Prestador.class));
    }

    @Test
    @DisplayName("Debe actualizar un prestador existente")
    void testUpsertDesdeDTO_ActualizaExistente() {
        Prestador prestadorExistente = new Prestador();
        prestadorExistente.setId(prestadorDto.getId());
        prestadorExistente.setNombre("Juan Viejo");

        when(prestadorRepository.findByExternalId(prestadorDto.getId())).thenReturn(Optional.of(prestadorExistente));
        when(prestadorRepository.save(any(Prestador.class))).thenAnswer(i -> i.getArgument(0));

        prestadorSyncService.upsertDesdeDTO(prestadorDto);
        verify(prestadorRepository, times(1)).save(prestadorExistente);
        verify(matchingPublisherService, times(1)).publishPrestadorUpsert(prestadorExistente);
    }

    @Test
    @DisplayName("No debe publicar en RabbitMQ si falla el guardado en la base de datos")
    void testUpsertDesdeDTO_NoPublicaSiFallaGuardado() {
        when(prestadorRepository.findByExternalId(prestadorDto.getId())).thenReturn(Optional.empty());
        when(prestadorRepository.save(any(Prestador.class))).thenThrow(new RuntimeException("Error de base de datos"));

        assertThrows(RuntimeException.class, () -> prestadorSyncService.upsertDesdeDTO(prestadorDto));

        verify(matchingPublisherService, never()).publishPrestadorUpsert(any());
    }
}
