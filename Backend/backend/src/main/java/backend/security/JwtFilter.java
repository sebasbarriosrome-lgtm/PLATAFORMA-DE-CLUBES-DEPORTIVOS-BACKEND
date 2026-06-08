package backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.Collections;

public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // ✅ Ignorar rutas públicas
        return path.equals("/usuarios/login") || path.equals("/usuarios/register");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        System.out.println("========== JWT FILTER ==========");
        System.out.println("PATH: " + request.getServletPath());
        System.out.println("HEADER: " + header);

        try {
            // ✅ Solo procesar si hay token
            if (header != null && header.startsWith("Bearer ")) {

                String token = header.substring(7);

                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(JwtUtil.getKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String email = claims.getSubject();
                System.out.println("EMAIL TOKEN: " + email);
                String rol = ((String) claims.get("rol")).toUpperCase();
                System.out.println("AUTH OK: " + rol);
                System.out.println("ROL NORMALIZADO:|" + rol);

                System.out.println("✅ AUTH OK: " + email);                

                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority("ROLE_" + rol);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                Collections.singletonList(authority)
                        );
                        System.out.println("AUTHORITIES: " + authentication.getAuthorities());

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // ✅ CLAVE: registrar autenticación correctamente
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // ✅ Para usar en el controller
                request.setAttribute("email", email);
                request.setAttribute("rol", rol);
            }

        } catch (Exception e) {
            System.out.println("❌ Token inválido");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token inválido");
            return;
        }

        filterChain.doFilter(request, response);
    }
}