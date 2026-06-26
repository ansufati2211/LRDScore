package com.rutadelsabor.core.annotations;

import com.rutadelsabor.core.models.enums.Modulo;

import java.lang.annotation.*;

// R0-4: evaluada por ModuloInterceptor. Puede aplicarse en clase o método.
// El chequeo es independiente de @PreAuthorize (R0-2): ambos deben pasar.
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiereModulo {
    Modulo value();
}
