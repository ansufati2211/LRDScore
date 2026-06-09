package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    // Método necesario para buscar clientes por su DNI o RUC desde el KDS/Caja
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);
    
}