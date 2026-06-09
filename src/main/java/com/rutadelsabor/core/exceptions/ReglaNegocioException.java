package com.rutadelsabor.core.exceptions;

public class ReglaNegocioException extends RuntimeException {
    
    // Constructor tradicional que ya usabas
    public ReglaNegocioException(String message) {
        super(message);
    }

    // NUEVO: Constructor para encadenar excepciones (Resuelve tu error de compilación)
    public ReglaNegocioException(String message, Throwable cause) {
        super(message, cause);
    }
}