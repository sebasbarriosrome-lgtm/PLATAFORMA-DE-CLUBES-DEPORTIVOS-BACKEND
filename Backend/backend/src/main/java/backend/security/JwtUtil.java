package backend.security;

import backend.entity.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET =
            "mi_clave_super_segura_mi_clave_super_segura_123456";

    private static final Key KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes());

    public static String generarToken(Usuario usuario) {

        return Jwts.builder()
                .setSubject(usuario.getEmail())
                .claim("rol", usuario.getRolGlobal().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Key getKey() {
        return KEY;
    }
}