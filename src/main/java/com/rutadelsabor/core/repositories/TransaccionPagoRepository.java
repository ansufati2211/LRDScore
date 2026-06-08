package com.rutadelsabor.core.repositories;
import com.rutadelsabor.core.models.entities.TransaccionPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {}