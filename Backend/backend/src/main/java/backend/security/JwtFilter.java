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
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;


import java.io.IOException;

public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        //  si no hay token
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(JwtUtil.getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String email = claims.getSubject();
            String rol = (String) claims.get("rol");

            System.out.println("🔐 Usuario autenticado: " + email);

            //  Crear autoridad (Spring necesita ROLE_...)
            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority("ROLE_" + rol);

            //  Crear autenticación real
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singletonList(authority)
                    );

            //  REGISTRAR EN SPRING SECURITY (🔥 CLAVE)
            SecurityContextHolder.getContext().setAuthentication(null); // limpiar primero
            SecurityContextHolder.getContext().setAuthentication(authentication);

            //  Mantener esto (lo usas en el controller)
            request.setAttribute("email", email);
            request.setAttribute("rol", rol);
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token inválido");
            return;
}

        filterChain.doFilter(request, response);
    }
}