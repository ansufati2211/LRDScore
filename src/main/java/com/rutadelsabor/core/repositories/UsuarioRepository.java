package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    // FIX CRÍTICO: Convertido a NATIVE QUERY con la tabla física 'usuarios' y columna 'empresa_id'.
    // Esto evita que Hibernate Multi-Tenancy le inyecte automáticamente el filtro de empresa
    // antes de que el usuario se haya autenticado.
    @Query(value = "SELECT u.empresa_id FROM usuarios u WHERE u.correo = :correo LIMIT 1", nativeQuery = true)
    Long findEmpresaIdByCorreo(@Param("correo") String correo);

    @Query(value = "SELECT * FROM usuarios u WHERE u.correo = :correo AND u.empresa_id = :empresaId AND u.estado_registro = true", nativeQuery = true)
    Optional<Usuario> findByCorreoYEmpresa(@Param("correo") String correo, @Param("empresaId") Long empresaId);

    // Busca la primera sede asignada al usuario
    @Query(value = "SELECT sede_id FROM usuario_sedes WHERE usuario_id = :usuarioId AND estado_registro = true LIMIT 1", nativeQuery = true)
    Optional<Long> findPrimerSedeIdByUsuarioId(@Param("usuarioId") Long usuarioId);
}