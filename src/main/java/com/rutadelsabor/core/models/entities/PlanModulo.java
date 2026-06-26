package com.rutadelsabor.core.models.entities;

import com.rutadelsabor.core.models.enums.Modulo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "plan_modulos",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_plan_modulo",
           columnNames = {"plan_id", "codigo_modulo"}
       ))
public class PlanModulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "codigo_modulo", nullable = false, length = 30)
    private Modulo codigoModulo;
}
