package com.rutadelsabor.core.repositories;
import com.rutadelsabor.core.models.entities.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;


public interface EmpresaRepository extends JpaRepository<Empresa, Long> {}