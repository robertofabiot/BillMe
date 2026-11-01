package com.example.billme.config;

import com.example.billme.model.Usuario;
import com.example.billme.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el primer usuario al arrancar, solo si la base no tiene ninguno.
 * Las credenciales salen de variables de entorno: nunca hay contraseñas en el código.
 */
@Slf4j
@Component
public class UsuarioInicial implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String nombre;

    public UsuarioInicial(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${billme.usuario-inicial.username:}") String username,
                          @Value("${billme.usuario-inicial.password:}") String password,
                          @Value("${billme.usuario-inicial.nombre:Administrador}") String nombre) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.nombre = nombre;
    }

    @Override
    public void run(ApplicationArguments args) {
        crearSiNoHayUsuarios();
    }

    public void crearSiNoHayUsuarios() {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            log.warn("No hay usuarios y faltan BILLME_USUARIO_INICIAL / BILLME_PASSWORD_INICIAL: "
                    + "nadie podrá iniciar sesión hasta definirlas y reiniciar.");
            return;
        }
        if (password.length() < Usuario.PASSWORD_MINIMO) {
            throw new IllegalStateException(
                    "BILLME_PASSWORD_INICIAL debe tener al menos " + Usuario.PASSWORD_MINIMO + " caracteres");
        }
        usuarioRepository.save(Usuario.builder()
                .username(username.trim())
                .password(passwordEncoder.encode(password))
                .nombre(nombre.trim())
                .build());
        log.info("Usuario inicial '{}' creado", username.trim());
    }
}
