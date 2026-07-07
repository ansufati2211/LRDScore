package com.rutadelsabor.core.repositories;
import com.rutadelsabor.core.models.entities.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {}