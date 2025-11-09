package com.example.demo.dto.catalog;

import com.example.demo.dto.PrestadorDireccionDTO;
import com.example.demo.entity.Habilidad;
import com.example.demo.entity.Prestador;
import com.example.demo.entity.PrestadorDireccion;
import com.example.demo.entity.Rubro;
import com.example.demo.entity.Zona;
import lombok.Builder;
import lombok.Value;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Value
@Builder
public class PrestadorCatalogDTO {

    Long internalId;
    Long externalId;
    String nombre;
    String apellido;
    String email;
    String telefono;
    String direccion;
    String estado;
    Double precioHora;
    Integer trabajosFinalizados;
    List<Short> calificacion;
    ZonaInfo zona;
    List<HabilidadInfo> habilidades;
    List<PrestadorDireccionDTO> direcciones;

    public static PrestadorCatalogDTO fromEntity(Prestador prestador) {
        if (prestador == null) {
            return null;
        }
        return PrestadorCatalogDTO.builder()
                .internalId(prestador.getInternalId())
                .externalId(prestador.getId())
                .nombre(prestador.getNombre())
                .apellido(prestador.getApellido())
                .email(prestador.getEmail())
                .telefono(prestador.getTelefono())
                .direccion(prestador.getDireccion())
                .estado(prestador.getEstado())
                .precioHora(prestador.getPrecioHora())
                .trabajosFinalizados(prestador.getTrabajosFinalizados())
                .calificacion(copyCalificaciones(prestador.getCalificacion()))
                .zona(ZonaInfo.from(prestador.getZona()))
                .habilidades(buildHabilidades(prestador))
                .direcciones(buildDirecciones(prestador))
                .build();
    }

    private static List<Short> copyCalificaciones(List<Short> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return List.copyOf(source);
    }

    private static List<HabilidadInfo> buildHabilidades(Prestador prestador) {
        if (prestador.getHabilidades() == null || prestador.getHabilidades().isEmpty()) {
            return List.of();
        }
        Map<String, HabilidadInfo> unique = new LinkedHashMap<>();
        for (Habilidad habilidad : prestador.getHabilidades()) {
            if (habilidad == null) {
                continue;
            }
            HabilidadInfo info = HabilidadInfo.from(habilidad);
            if (info == null) {
                continue;
            }
            String key = buildHabilidadKey(habilidad);
            unique.putIfAbsent(key, info);
        }
        return List.copyOf(unique.values());
    }

    private static String buildHabilidadKey(Habilidad habilidad) {
        if (habilidad.getId() != null) {
            return "ID:" + habilidad.getId();
        }
        if (habilidad.getExternalId() != null) {
            return "EXT:" + habilidad.getExternalId();
        }
        String nombre = habilidad.getNombre() != null ? habilidad.getNombre().trim().toLowerCase() : "sin_nombre";
        String rubroId = habilidad.getRubro() != null
                ? String.valueOf(habilidad.getRubro().getId() != null ? habilidad.getRubro().getId() : habilidad.getRubro().getExternalId())
                : "sin_rubro";
        return nombre + "#" + rubroId;
    }

    private static List<PrestadorDireccionDTO> buildDirecciones(Prestador prestador) {
        List<PrestadorDireccion> direcciones = prestador.getDirecciones();
        if (direcciones == null || direcciones.isEmpty()) {
            return List.of();
        }
        Map<String, PrestadorDireccionDTO> unique = new LinkedHashMap<>();
        for (PrestadorDireccion dir : direcciones) {
            if (dir == null) {
                continue;
            }
            PrestadorDireccionDTO dto = PrestadorDireccionDTO.builder()
                    .state(dir.getState())
                    .city(dir.getCity())
                    .street(dir.getStreet())
                    .number(dir.getNumber())
                    .floor(dir.getFloor())
                    .apartment(dir.getApartment())
                    .build();
            String key = (dto.getState() + "|" + dto.getCity() + "|" + dto.getStreet() + "|" + dto.getNumber()
                    + "|" + dto.getFloor() + "|" + dto.getApartment()).toLowerCase();
            unique.putIfAbsent(key, dto);
        }
        return List.copyOf(unique.values());
    }

    @Value
    @Builder
    public static class ZonaInfo {
        Long id;
        Long externalId;
        String nombre;

        static ZonaInfo from(Zona zona) {
            if (zona == null) {
                return null;
            }
            return ZonaInfo.builder()
                    .id(zona.getId())
                    .externalId(zona.getExternalId())
                    .nombre(zona.getNombre())
                    .build();
        }
    }

    @Value
    @Builder
    public static class HabilidadInfo {
        Long id;
        Long externalId;
        String nombre;
        RubroInfo rubro;

        static HabilidadInfo from(Habilidad habilidad) {
            if (habilidad == null) {
                return null;
            }
            return HabilidadInfo.builder()
                    .id(habilidad.getId())
                    .externalId(habilidad.getExternalId())
                    .nombre(habilidad.getNombre())
                    .rubro(RubroInfo.from(habilidad.getRubro()))
                    .build();
        }
    }

    @Value
    @Builder
    public static class RubroInfo {
        Long id;
        Long externalId;
        String nombre;

        static RubroInfo from(Rubro rubro) {
            if (rubro == null) {
                return null;
            }
            return RubroInfo.builder()
                    .id(rubro.getId())
                    .externalId(rubro.getExternalId())
                    .nombre(rubro.getNombre())
                    .build();
        }
    }
}
