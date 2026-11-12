package ifg.urutai.classvision.cvauthenticationservice.security;

import ifg.urutai.classvision.cvauthenticationservice.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:minha-chave-secreta-ultra-segura-para-tokens-jwt-cv-authentication-service}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // 24 horas em milissegundos
    private long jwtExpirationInMs;

    /**
     * Gera o token com os dados que o front e os demais microsserviços usam
     * para controlar acesso. O payload do JWT é apenas Base64 (legível por
     * qualquer um): nunca incluir dados sensíveis como senha, CPF ou nascimento.
     */
    public String generateToken(Usuario usuario) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("id", usuario.getId());
        claims.put("nome", usuario.getNome());
        claims.put("email", usuario.getEmail());
        // "role" sem prefixo, para comparação direta no front/microsserviços.
        claims.put("role", usuario.getRole() != null ? usuario.getRole().getNome() : null);
        // "roles" com prefixo ROLE_, no formato esperado pelo Spring Security.
        claims.put("instituicaoId", usuario.getInstituicao() != null ? usuario.getInstituicao().getId() : null);
        claims.put("roles", usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
        return createToken(claims, usuario.getEmail());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public long getExpirationTime() {
        return jwtExpirationInMs;
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }


    public Boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}

