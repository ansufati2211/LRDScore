package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByCorreo(String correo);
    
    boolean existsByCorreo(String correo);

    // NUEVO: Consulta ultraligera solo para averiguar a qué empresa pertenece el correo
    @Query(value = "SELECT empresa_id FROM usuarios WHERE correo = :correo AND estado_registro = true", nativeQuery = true)
    Long findEmpresaIdByCorreo(@Param("correo") String correo);
}