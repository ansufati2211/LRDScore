package com.rutadelsabor.core.controllers;

import com.rutadelsabor.core.config.tenant.TenantContext;
import com.rutadelsabor.core.dto.request.EmpresaRequestDTO;
import com.rutadelsabor.core.exceptions.RecursoNoEncontradoException;
import com.rutadelsabor.core.exceptions.ReglaNegocioException;
import com.rutadelsabor.core.models.entities.Empresa;
import com.rutadelsabor.core.models.entities.Plan;
import com.rutadelsabor.core.models.entities.Suscripcion;
import com.rutadelsabor.core.models.enums.EstadoSuscripcion;
import com.rutadelsabor.core.repositories.EmpresaRepository;
import com.rutadelsabor.core.repositories.PlanRepository;
import com.rutadelsabor.core.repositories.SuscripcionRepository;
import com.rutadelsabor.core.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/empresas") 
public class EmpresaController {

    private final EmpresaRepository empresaRepository;
    private final PlanRepository planRepository;
    private final SuscripcionRepository suscripcionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public EmpresaController(EmpresaRepository empresaRepository, 
                             PlanRepository planRepository, 
                             SuscripcionRepository suscripcionRepository,
                             UsuarioRepository usuarioRepository,
                             PasswordEncoder passwordEncoder,
                             JdbcTemplate jdbcTemplate) {
        this.empresaRepository = empresaRepository;
        this.planRepository = planRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listarTodas() {
        try {
            List<Empresa> empresas = empresaRepository.findAll();
            
            List<Map<String, Object>> response = empresas.stream().map(emp -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", emp.getId());
                map.put("nombreComercial", emp.getNombreComercial());
                map.put("ruc", emp.getRuc());
                map.put("direccion", emp.getDireccion());
                map.put("estadoRegistro", emp.getEstadoRegistro());
                
                if (emp.getSuscripcionVigente() != null) {
                    Map<String, Object> sub = new HashMap<>();
                    sub.put("id", emp.getSuscripcionVigente().getId());
                    sub.put("estado", emp.getSuscripcionVigente().getEstado());
                    if (emp.getSuscripcionVigente().getPlan() != null) {
                        Map<String, Object> plan = new HashMap<>();
                        plan.put("id", emp.getSuscripcionVigente().getPlan().getId());
                        plan.put("nombre", emp.getSuscripcionVigente().getPlan().getNombre());
                        plan.put("precioMensual", emp.getSuscripcionVigente().getPlan().getPrecioMensual());
                        sub.put("plan", plan);
                    }
                    map.put("suscripcionVigente", sub);
                }
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error 500 en Backend: " + e.getMessage());
        }
    }

    @GetMapping("/mi-empresa")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA', 'ROLE_GERENTE_SEDE', 'ROLE_CAJERO')")
    public ResponseEntity<Empresa> obtenerMiEmpresa() {
        Long empresaId = TenantContext.getCurrentTenant();
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));
        return ResponseEntity.ok(empresa);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Empresa> registrarEmpresa(@RequestBody EmpresaRequestDTO request) {
        Empresa empresa = new Empresa();
        empresa.setNombreComercial(request.getNombreComercial());
        empresa.setRuc(request.getRuc());
        empresa.setDireccion(request.getDireccion());
        empresa.setEstadoRegistro(true);
        return new ResponseEntity<>(empresaRepository.save(empresa), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_EMPRESA')")
    public ResponseEntity<Empresa> actualizarEmpresa(@PathVariable Long id, @RequestBody EmpresaRequestDTO request) {
        Long tenantId = TenantContext.getCurrentTenant();
        Long objetivo = (tenantId != null) ? tenantId : id;

        return empresaRepository.findById(objetivo)
                .map(empresa -> {
                    if (request.getNombreComercial() != null) empresa.setNombreComercial(request.getNombreComercial());
                    if (request.getRuc() != null) empresa.setRuc(request.getRuc());
                    if (request.getDireccion() != null) empresa.setDireccion(request.getDireccion());
                    return ResponseEntity.ok(empresaRepository.save(empresa));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> darDeBajaEmpresa(@PathVariable Long id) {
        if (id == 1L) {
            throw new ReglaNegocioException("Operación denegada: No se puede suspender la Empresa Matriz (ID 1) porque bloquearía el acceso del Súper Administrador.");
        }

        return empresaRepository.findById(id)
                .map(empresa -> {
                    empresa.setEstadoRegistro(false);
                    empresaRepository.save(empresa);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> activarEmpresa(@PathVariable Long id) {
        return empresaRepository.findById(id)
                .map(empresa -> {
                    empresa.setEstadoRegistro(true);
                    empresaRepository.save(empresa);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/plan")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> cambiarPlanEmpresa(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        Long nuevoPlanId = request.get("planId");
        
        Empresa empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));

        Plan plan = planRepository.findById(nuevoPlanId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan no encontrado"));

        Suscripcion planAnterior = empresa.getSuscripcionVigente();
        if (planAnterior != null) {
            planAnterior.setEstado(EstadoSuscripcion.VENCIDA);
            planAnterior.setFechaFin(LocalDate.now()); 
            suscripcionRepository.save(planAnterior);
        }

        Suscripcion nuevaSuscripcion = new Suscripcion();
        nuevaSuscripcion.setEmpresa(empresa);
        nuevaSuscripcion.setPlan(plan);
        nuevaSuscripcion.setEstado(EstadoSuscripcion.ACTIVA); 
        nuevaSuscripcion.setFechaInicio(LocalDate.now()); 
        
        Suscripcion guardada = suscripcionRepository.save(nuevaSuscripcion);

        empresa.setSuscripcionVigente(guardada);
        empresaRepository.save(empresa);

        return ResponseEntity.ok(empresa);
    }

    @PostMapping("/onboarding")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> onboardingSaaS(@RequestBody Map<String, String> payload) {
        
        if (usuarioRepository.findEmpresaIdByCorreo(payload.get("adminCorreo")) != null) {
            throw new ReglaNegocioException("El correo del administrador ya existe en el sistema.");
        }

        Empresa empresa = new Empresa();
        empresa.setNombreComercial(payload.get("nombreComercial"));
        empresa.setRuc(payload.get("ruc"));
        empresa.setDireccion(payload.get("direccion"));
        empresa.setEstadoRegistro(true);
        Empresa empresaGuardada = empresaRepository.save(empresa);

        Long planId = Long.valueOf(payload.get("planId"));
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan no encontrado"));
        
        Suscripcion sub = new Suscripcion();
        sub.setEmpresa(empresaGuardada);
        sub.setPlan(plan);
        sub.setEstado(EstadoSuscripcion.ACTIVA);
        sub.setFechaInicio(LocalDate.now());
        Suscripcion subGuardada = suscripcionRepository.save(sub);
        
        empresaGuardada.setSuscripcionVigente(subGuardada);
        empresaRepository.save(empresaGuardada);

        String adminNombre = payload.get("adminNombre");
        String adminCorreo = payload.get("adminCorreo");
        String adminPassword = passwordEncoder.encode(payload.get("adminPassword"));
        
        jdbcTemplate.update(
            "INSERT INTO usuarios (nombre, correo, password_hash, rol, estado_registro, empresa_id, created_at, updated_at) " +
            "VALUES (?, ?, ?, 'ROLE_ADMIN_EMPRESA', true, ?, NOW(), NOW())",
            adminNombre, adminCorreo, adminPassword, empresaGuardada.getId()
        );

        Map<String, Object> respuestaSegura = new HashMap<>();
        respuestaSegura.put("id", empresaGuardada.getId());
        respuestaSegura.put("nombreComercial", empresaGuardada.getNombreComercial());
        respuestaSegura.put("mensaje", "Cliente y administrador creados con éxito");

        return ResponseEntity.status(HttpStatus.CREATED).body(respuestaSegura);
    }
}