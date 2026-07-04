package com.rutadelsabor.core.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Contraseña incorrecta
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "No Autorizado", "Correo o contraseña incorrectos", request, null);
    }

    // 2. Correo no existe en BD
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleUserNotFound(UsernameNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "No Encontrado", ex.getMessage(), request, null);
    }

    // 3. Recurso no encontrado
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> handleRecursoNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Recurso No Encontrado", ex.getMessage(), request, null);
    }

    // 4. Reglas de negocio
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> handleReglasNegocio(ReglaNegocioException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Error de Regla de Negocio", ex.getMessage(), request, null);
    }

    // 4b. Stock insuficiente al confirmar pedido (R3-1) — devuelve detalle por insumo
    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<StockInsuficienteResponseDTO> handleStockInsuficiente(StockInsuficienteException ex, HttpServletRequest request) {
        StockInsuficienteResponseDTO body = new StockInsuficienteResponseDTO(
                LocalDateTime.now(ZoneId.systemDefault()), 422, "Stock Insuficiente",
                ex.getMessage(), request.getRequestURI(), "STOCK_INSUFICIENTE", ex.getFaltantes());
        return new ResponseEntity<>(body, HttpStatusCode.valueOf(422));
    }

    // 5. Validación de campos @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, "Error de Validación", mensaje, request, null);
    }

    // 6. Rol insuficiente (@PreAuthorize) — R0-2: código distinto de MODULO_NO_HABILITADO
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Acceso Denegado", "No tienes permiso para realizar esta acción", request, "ACCESO_DENEGADO");
    }

    // 7. Plan no incluye el módulo — E0-2: código MODULO_NO_HABILITADO
    @ExceptionHandler(ModuloNoHabilitadoException.class)
    public ResponseEntity<ErrorResponseDTO> handleModuloNoHabilitado(ModuloNoHabilitadoException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Módulo No Habilitado", ex.getMessage(), request, "MODULO_NO_HABILITADO");
    }

    // 8. Suscripción vencida + escritura sobre módulo core — E0-1
    @ExceptionHandler(SuscripcionVencidaException.class)
    public ResponseEntity<ErrorResponseDTO> handleSuscripcionVencida(SuscripcionVencidaException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Suscripción Vencida", ex.getMessage(), request, "SUSCRIPCION_VENCIDA");
    }

    // 9. Error general — nunca exponer detalles internos al cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error Interno del Servidor",
                "Ha ocurrido un error inesperado. Por favor contacte al administrador.", request, null);
    }

    private ResponseEntity<ErrorResponseDTO> build(HttpStatus status, String error, String message,
                                                    HttpServletRequest request, String codigo) {
        ErrorResponseDTO body = new ErrorResponseDTO(
                LocalDateTime.now(ZoneId.systemDefault()), status.value(), error, message, request.getRequestURI(), codigo);
        return new ResponseEntity<>(body, status);
    }
}
