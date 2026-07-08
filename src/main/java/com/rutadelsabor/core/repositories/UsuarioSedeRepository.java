package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.UsuarioSede;
import com.rutadelsabor.core.models.entities.UsuarioSedeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioSedeRepository extends JpaRepository<UsuarioSede, UsuarioSedeId> {
    List<UsuarioSede> findByUsuarioIdAndEstadoRegistroTrue(Long usuarioId);
    List<UsuarioSede> findBySedeIdAndEstadoRegistroTrue(Long sedeId);
}