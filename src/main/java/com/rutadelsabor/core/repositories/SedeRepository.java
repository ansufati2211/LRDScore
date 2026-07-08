package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Sede;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface SedeRepository extends JpaRepository<Sede, Long> {
    Optional<Sede> findByCodigoEstablecimiento(String codigoEstablecimiento);
}