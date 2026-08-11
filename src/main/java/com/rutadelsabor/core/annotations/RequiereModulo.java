package com.rutadelsabor.core.annotations;

import com.rutadelsabor.core.models.enums.Modulo;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiereModulo {
    Modulo value();
}
