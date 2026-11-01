package com.example.billme.config;

import com.example.billme.repository.UsuarioRepository;
import com.example.billme.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioInicialTest extends IntegrationTest {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void creaElPrimerUsuarioSoloSiNoHayNinguno() {
        UsuarioInicial inicial = new UsuarioInicial(usuarioRepository, passwordEncoder,
                "juan", "clave-inicial-1", "Juan Pérez");

        inicial.crearSiNoHayUsuarios();
        inicial.crearSiNoHayUsuarios();

        assertThat(usuarioRepository.count()).isEqualTo(1);
        var usuario = usuarioRepository.findByUsername("juan").orElseThrow();
        assertThat(passwordEncoder.matches("clave-inicial-1", usuario.getPassword())).isTrue();
        assertThat(usuario.isActivo()).isTrue();
    }

    @Test
    void sinVariablesNoCreaNada() {
        new UsuarioInicial(usuarioRepository, passwordEncoder, "", "", "Administrador").crearSiNoHayUsuarios();

        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void rechazaUnaPasswordInicialCorta() {
        UsuarioInicial inicial = new UsuarioInicial(usuarioRepository, passwordEncoder, "juan", "1234", "Juan");

        assertThatThrownBy(inicial::crearSiNoHayUsuarios).isInstanceOf(IllegalStateException.class);
    }
}
