package com.company.util;

import com.company.dto.ProfileJwtDTO;
import com.company.enums.ProfileRole;
import com.company.exception.AppBadRequestException;
import com.company.exception.AppForbiddenException;
import com.company.exception.TokenNotValidException;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Optional;

@Slf4j
public class JwtUtil {
    private final static String secretKey = "key for jwt";

    public static String encode(String id, ProfileRole role) {
        return doEncode(id, null, role, 60);
    }

    public static String encode(String id) {
        return doEncode(id, null, null, 3);
    }

    public static String encodeEmail(String id, String email, ProfileRole role) {
        return doEncode(id, email, role, 3);
    }

    public static String doEncode(String id, String email, ProfileRole role, long minute) {
        JwtBuilder jwtBuilder = Jwts.builder();
        jwtBuilder.setSubject(id);
        jwtBuilder.setIssuedAt(new Date());
        jwtBuilder.signWith(SignatureAlgorithm.HS256, secretKey);
        jwtBuilder.setExpiration(new Date(System.currentTimeMillis() + (minute * 60 * 1000)));
        jwtBuilder.setIssuer("mazgi production");

        if (Optional.ofNullable(role).isPresent()) {
            jwtBuilder.claim("role", role);
        }

        if (Optional.ofNullable(email).isPresent()) {
            jwtBuilder.claim("email", email);
        }

        return jwtBuilder.compact();
    }

    public static ProfileJwtDTO decode(String jwt) {
        try {
            JwtParser jwtParser = Jwts.parser();

            jwtParser.setSigningKey(secretKey);

            Jws<Claims> jws = jwtParser.parseClaimsJws(jwt);

            Claims claims = jws.getBody();

            Integer id = Integer.valueOf(claims.getSubject());
            String role = String.valueOf(claims.get("role"));
            String email = String.valueOf(claims.get("email"));

            return new ProfileJwtDTO(id, ProfileRole.valueOf(role), email);
        } catch (JwtException e) {
            log.warn("JWT invalid {}", jwt);
            throw new AppBadRequestException("JWT invalid!");
        }
    }

    public static Integer decodeAndGetId(String jwt) {
        try {
            JwtParser jwtParser = Jwts.parser();

            jwtParser.setSigningKey(secretKey);
            Jws<Claims> jws = jwtParser.parseClaimsJws(jwt);

            Claims claims = jws.getBody();

            return Integer.valueOf(claims.getSubject());
        } catch (JwtException e) {
            log.warn("JWT invalid {}", jwt);
            throw new AppBadRequestException("JWT invalid!");
        }
    }


    public static Integer getIdFromHeader(HttpServletRequest request, ProfileRole... requiredRoles) {
        try {
            ProfileJwtDTO dto = (ProfileJwtDTO) request.getAttribute("profileJwtDTO");
            if (requiredRoles == null || requiredRoles.length == 0) {
                return dto.getId();
            }
            for (ProfileRole role : requiredRoles) {
                if (role.equals(dto.getRole())) {
                    return dto.getId();
                }
            }
        } catch (RuntimeException e) {
            log.warn("Not Authorized");
            throw new TokenNotValidException("Not Authorized!");
        }
        log.warn("Not Access");
        throw new AppForbiddenException("Not Access!");
    }


}
