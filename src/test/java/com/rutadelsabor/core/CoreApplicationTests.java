package com.rutadelsabor.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
class CoreApplicationTests {

    // Inyectamos el BCryptPasswordEncoder que creamos en PasswordEncoderConfig
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranque bien
    }

    @Test
    void probarEncriptadorBcrypt() {
        // 1. Definimos la contraseña en texto plano
        String rawPassword = "admin123";
        
        // 2. Encriptamos la contraseña
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        // Imprimimos el resultado en la consola para que puedas copiarlo a tu BD
        System.out.println("\n=========================================================");
        System.out.println("🔐 CONTRASEÑA ORIGINAL : " + rawPassword);
        System.out.println("🛡️ HASH BCRYPT GENERADO: " + encodedPassword);
        System.out.println("=========================================================\n");
        
        // 3. Afirmamos que la encriptación funcionó (no son iguales)
        assertNotEquals(rawPassword, encodedPassword);
        
        // 4. Probamos la magia de BCrypt: Verificamos si coinciden
        // Nota: Nunca uses ".equals()" con BCrypt, siempre usa ".matches()"
        boolean isMatch = passwordEncoder.matches(rawPassword, encodedPassword);
        
        // Si isMatch es falso, la prueba fallará
        assertTrue(isMatch, "El encriptador falló al validar la contraseña.");
    }
}