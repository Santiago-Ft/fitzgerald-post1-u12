package com.empresa.reembolsos.adaptadores.rest;

import com.empresa.reembolsos.aplicacion.SolicitudInvalidaException;
import com.empresa.reembolsos.aplicacion.SolicitudReembolsoService;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudReembolsoController {
    private final SolicitudReembolsoService servicio;

    public SolicitudReembolsoController(SolicitudReembolsoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<SolicitudReembolso> crear(@RequestBody SolicitudReembolso solicitud) {
        return ResponseEntity.ok(servicio.crear(solicitud));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<SolicitudReembolso> cambiarEstado(@PathVariable Long id, @RequestBody EstadoSolicitud nuevoEstado) {
        return ResponseEntity.ok(servicio.cambiarEstado(id, nuevoEstado));
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Map<String, String>>manejarSolicitudInvalida(SolicitudInvalidaException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }
}