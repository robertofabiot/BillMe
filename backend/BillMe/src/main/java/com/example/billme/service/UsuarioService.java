package com.example.billme.service;

import com.example.billme.dto.UsuarioRequest;
import com.example.billme.dto.UsuarioResponse;
import com.example.billme.model.Usuario;
import com.example.billme.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .sorted(Comparator.comparing(Usuario::getNombre))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        String username = request.username().trim();
        if (usuarioRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre de usuario");
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .username(username)
                .nombre(request.nombre().trim())
                .password(passwordEncoder.encode(request.password()))
                .build());
        return toResponse(usuario);
    }

    @Transactional
    public void cambiarMiPassword(String username, String actual, String nueva) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!passwordEncoder.matches(actual, usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
        }
        usuario.setPassword(passwordEncoder.encode(nueva));
    }

    @Transactional
    public void restablecerPassword(UUID id, String nueva, String usernameActual) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (usuario.getUsername().equals(usernameActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para tu propia contraseña usa \"Cambiar mi contraseña\"");
        }
        usuario.setPassword(passwordEncoder.encode(nueva));
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getUsername(), usuario.getNombre(), usuario.isActivo());
    }
}
