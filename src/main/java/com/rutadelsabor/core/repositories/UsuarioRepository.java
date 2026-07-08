package com.rutadelsabor.core.repositories;

import com.rutadelsabor.core.models.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;


public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
Optional<Usuario> findByCorreo(String correo);

    // FIX: Método restaurado para UsuarioServiceImpl
    boolean existsByCorreo(String correo);
    @Query("SELECT u.empresaId FROM Usuario u WHERE u.correo = :correo")
    Long findEmpresaIdByCorreo(@Param("correo") String correo);

    @Query(value = "SELECT * FROM usuarios u WHERE u.correo = :correo AND u.empresa_id = :empresaId AND u.estado_registro = true", nativeQuery = true)
    Optional<Usuario> findByCorreoYEmpresa(@Param("correo") String correo, @Param("empresaId") Long empresaId);

    // NUEVO MÉTODO PARA MULTI-SEDE: Busca la primera sede asignada al usuario
    @Query(value = "SELECT sede_id FROM usuario_sedes WHERE usuario_id = :usuarioId AND estado_registro = true LIMIT 1", nativeQuery = true)
    Optional<Long> findPrimerSedeIdByUsuarioId(@Param("usuarioId") Long usuarioId);
}