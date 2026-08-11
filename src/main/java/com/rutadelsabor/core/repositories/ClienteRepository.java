package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;


public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);
    
}