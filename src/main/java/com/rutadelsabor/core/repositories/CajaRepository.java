package com.rutadelsabor.core.repositories;
import com.rutadelsabor.core.models.entities.SesionCaja;
import com.rutadelsabor.core.models.enums.EstadoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CajaRepository extends JpaRepository<SesionCaja, Long> {
    // Busca si el cajero ya tiene una caja abierta
    Optional<SesionCaja> findByCajeroIdAndEstado(Long cajeroId, EstadoCaja estado);
}