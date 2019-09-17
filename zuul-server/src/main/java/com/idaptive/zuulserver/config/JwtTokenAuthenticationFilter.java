package com.idaptive.zuulserver.config;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class JwtTokenAuthenticationFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String jwtKey = "kjaspdjjjslaytl";
		String storeCookie = request.getHeader("cookie");
		String token = null;
		if (storeCookie != null) {
			String[] cookieArr = storeCookie.split(";");
			for (String cookie : cookieArr) {
				String key = cookie.split("=")[0];
				if (key.equals(" JwtToken") || key.equals("JwtToken")) {
					token = cookie.split("=")[1];
				}
			}
			try {
				Claims claims = Jwts.parser().setSigningKey(DatatypeConverter.parseBase64Binary(jwtKey))
						.parseClaimsJws(token).getBody();

				String username = claims.getSubject();
				if (username != null) {
					@SuppressWarnings("unchecked")
					List<String> authorities = (List<String>) claims.get("authorities");
					UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null,
							authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
					SecurityContextHolder.getContext().setAuthentication(auth);
				}
			} catch (Exception e) {
				SecurityContextHolder.clearContext();
			}
		}
		filterChain.doFilter(request, response);
	}
}
