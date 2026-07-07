package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByCorreo(String correo);
    
    boolean existsByCorreo(String correo);

    // NUEVO: Consulta ultraligera solo para averiguar a qué empresa pertenece el correo
    @Query(value = "SELECT empresa_id FROM usuarios WHERE correo = :correo AND estado_registro = true", nativeQuery = true)
    Long findEmpresaIdByCorreo(@Param("correo") String correo);

    // Nativa a propósito: bypassa el filtro @TenantId de Hibernate, que con spring.jpa.open-in-view
    // (activo por defecto) queda fijado en -1 desde el inicio del request — antes de que
    // TenantContext.setCurrentTenant() se ejecute durante la autenticación. Ver UserDetailsServiceImpl.
    @Query(value = "SELECT * FROM usuarios WHERE correo = :correo AND empresa_id = :empresaId", nativeQuery = true)
    Optional<Usuario> findByCorreoYEmpresa(@Param("correo") String correo, @Param("empresaId") Long empresaId);
}