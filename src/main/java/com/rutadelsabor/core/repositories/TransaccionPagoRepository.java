package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.TransaccionPago;
import com.rutadelsabor.core.models.enums.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {

    // Suma solo los pagos en EFECTIVO de una sesión — dinero físico que entra a la caja
    @Query("SELECT COALESCE(SUM(t.monto), 0) FROM TransaccionPago t WHERE t.sesionCaja.id = :sesionId AND t.metodoPago = :metodo")
    BigDecimal sumarPorSesionYMetodo(@Param("sesionId") Long sesionId, @Param("metodo") MetodoPago metodo);
}