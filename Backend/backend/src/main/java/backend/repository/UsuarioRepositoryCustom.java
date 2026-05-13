package backend.repository;

import backend.entity.Usuario;

import java.util.Optional;

public interface UsuarioRepositoryCustom {

    void registrarUsuario(
            String nombre,
            String apellido,
            String email,
            String password
    );

    Optional<Usuario> buscarPorEmail(String email);
}