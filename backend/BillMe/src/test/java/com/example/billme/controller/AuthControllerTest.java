package com.example.billme.controller;

import com.example.billme.model.Usuario;
import com.example.billme.repository.UsuarioRepository;
import com.example.billme.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends IntegrationTest {

    private static final String PASSWORD = "clave-de-prueba";

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    String jwtSecret;

    Usuario juan;

    @BeforeEach
    void crearUsuarios() {
        juan = crearUsuario("juan", "Juan Pérez");
    }

    @Test
    void loginCorrectoDevuelveTokenSinRol() throws Exception {
        login("juan", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.nombre").value("Juan Pérez"))
                .andExpect(jsonPath("$.rol").doesNotExist());
    }

    @Test
    void loginIncorrectoResponde401ConMensaje() throws Exception {
        login("juan", "otra-clave-cualquiera")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void conTokenValidoEntra() throws Exception {
        mockMvc.perform(get("/api/clientes").header("Authorization", "Bearer " + token("juan", PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void tokenVencidoResponde401() throws Exception {
        Date haceUnaHora = new Date(System.currentTimeMillis() - 3_600_000);
        String vencido = Jwts.builder()
                .subject("juan")
                .issuedAt(new Date(haceUnaHora.getTime() - 60_000))
                .expiration(haceUnaHora)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)), Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(get("/api/clientes").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenMalFormadoResponde401() throws Exception {
        mockMvc.perform(get("/api/clientes").header("Authorization", "Bearer esto-no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioInactivoNoPuedeEntrar() throws Exception {
        String token = token("juan", PASSWORD);
        juan.setActivo(false);
        usuarioRepository.save(juan);

        mockMvc.perform(get("/api/clientes").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        login("juan", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void cambiarMiPasswordExigeLaActualYReemplazaLaAnterior() throws Exception {
        String token = token("juan", PASSWORD);

        cambiarMiPassword(token, "no-es-la-actual", "nueva-clave-123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña actual no es correcta"));
        cambiarMiPassword(token, PASSWORD, "corta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("nueva"));
        cambiarMiPassword(token, PASSWORD, "nueva-clave-123")
                .andExpect(status().isNoContent());

        login("juan", PASSWORD).andExpect(status().isUnauthorized());
        login("juan", "nueva-clave-123").andExpect(status().isOk());
    }

    @Test
    void sePuedeRestablecerLaPasswordDeOtroPeroNoLaPropia() throws Exception {
        Usuario maria = crearUsuario("maria", "María López");
        String token = token("juan", PASSWORD);

        mockMvc.perform(put("/api/usuarios/{id}/password", maria.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nueva\": \"clave-nueva-maria\"}"))
                .andExpect(status().isNoContent());
        login("maria", "clave-nueva-maria").andExpect(status().isOk());

        mockMvc.perform(put("/api/usuarios/{id}/password", juan.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nueva\": \"otra-clave-123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearUsuarioRepetidoResponde409() throws Exception {
        String token = token("juan", PASSWORD);
        String cuerpo = "{\"username\": \"maria\", \"nombre\": \"María\", \"password\": \"clave-de-maria\"}";

        mockMvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("maria"));
        mockMvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict());
        login("maria", "clave-de-maria").andExpect(status().isOk());
    }

    private Usuario crearUsuario(String username, String nombre) {
        return usuarioRepository.save(Usuario.builder()
                .username(username)
                .nombre(nombre)
                .password(passwordEncoder.encode(PASSWORD))
                .build());
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}"));
    }

    private String token(String username, String password) throws Exception {
        String respuesta = login(username, password).andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private ResultActions cambiarMiPassword(String token, String actual, String nueva) throws Exception {
        return mockMvc.perform(put("/api/usuarios/yo/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\": \"" + actual + "\", \"nueva\": \"" + nueva + "\"}"));
    }
}
