package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.annotations.RequiereModulo;
import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.request.ReservaRequestDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.models.entities.Reserva;
import com.rutadelsabor.core.models.enums.EstadoReserva;
import com.rutadelsabor.core.models.enums.Modulo;
import com.rutadelsabor.core.repositories.ReservaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO', 'ROLE_MOZO')")
public class ReservaController {

    private final ReservaRepository reservaRepository;

    public ReservaController(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    // --- NUEVO ENDPOINT QUE ACABAMOS DE AGREGAR ---
    @GetMapping
    @RequiereModulo(Modulo.RESERVAS)
    public ResponseEntity<List<Reserva>> obtenerTodasLasReservas(@RequestParam(required = false) Long sedeId) {
        Long sedeEfectiva = TenantContext.resolverSedeEfectiva(sedeId);
        List<Reserva> reservas = reservaRepository.findBySedeIdOrderByFechaHoraAsc(sedeEfectiva);
        return ResponseEntity.ok(reservas);
    }
    // ----------------------------------------------

    @GetMapping("/del-dia")
    @RequiereModulo(Modulo.RESERVAS)
    public ResponseEntity<List<Reserva>> obtenerReservasDelDia() {
        Long sedeId = TenantContext.resolverSedeEfectiva(null);
        LocalDate hoy = LocalDate.now();
        List<Reserva> reservas = reservaRepository.findBySedeIdAndFechaHoraBetweenOrderByFechaHoraAsc(
                sedeId, hoy.atStartOfDay(), hoy.atTime(23, 59, 59));
        return ResponseEntity.ok(reservas);
    }

    @PostMapping
    @RequiereModulo(Modulo.RESERVAS)
    public ResponseEntity<Reserva> crearReserva(@RequestBody ReservaRequestDTO dto) {
        Long sedeId = TenantContext.resolverSedeEfectiva(dto.getSedeId());
        
        Reserva reserva = new Reserva();
        reserva.setEmpresaId(TenantContext.getCurrentTenant());
        reserva.setSedeId(sedeId);
        reserva.setNombreCliente(dto.getNombreCliente());
        reserva.setTelefonoCliente(dto.getTelefonoCliente());
        reserva.setFechaHora(dto.getFechaHora());
        reserva.setCantidadPersonas(dto.getCantidadPersonas());
        reserva.setNotas(dto.getNotas());
        
        return ResponseEntity.ok(reservaRepository.save(reserva));
    }

    @PutMapping("/{id}/estado")
    @RequiereModulo(Modulo.RESERVAS)
    public ResponseEntity<Reserva> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        reserva.setEstado(EstadoReserva.valueOf(estado.toUpperCase()));
        return ResponseEntity.ok(reservaRepository.save(reserva));
    }
}