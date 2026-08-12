package com.example.billme.service;

import com.example.billme.dto.AuthRequest;
import com.example.billme.dto.AuthResponse;
import com.example.billme.enums.Rol;
import com.example.billme.model.Usuario;
import com.example.billme.repository.UsuarioRepository;
import com.example.billme.security.JwtService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(AuthRequest request) {
        // Autentica las credenciales con Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        
        // Si no lanza excepción, las credenciales son válidas
        Usuario usuario = usuarioRepository.findByUsername(request.username())
                .orElseThrow();
                
        // Genera el token JWT
        String token = jwtService.generateToken(usuario);
        
        return new AuthResponse(token, usuario.getUsername(), usuario.getNombre(), usuario.getRol());
    }

    /**
     * Se ejecuta al arrancar el servidor.
     * Crea un usuario administrador por defecto si la base de datos está vacía.
     */
    @PostConstruct
    public void seedAdminUser() {
        if (!usuarioRepository.existsByUsername("admin")) {
            Usuario admin = Usuario.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .nombre("Administrador del Sistema")
                    .rol(Rol.ADMIN)
                    .build();
            usuarioRepository.save(admin);
            System.out.println("✅ Usuario ADMIN por defecto creado (admin / admin123)");
        }
    }
}
