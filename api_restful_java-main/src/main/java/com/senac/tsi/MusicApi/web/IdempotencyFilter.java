package com.senac.tsi.MusicApi.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

// prepara os POST que trazem X-Idempotency-Key: guarda o corpo da requisicao e o da resposta,
// para o IdempotencyInterceptor comparar e salvar; a logica fica toda no interceptor
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || request.getHeader(IdempotencyInterceptor.HEADER) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(new CachedBodyHttpServletRequest(request), cachingResponse);
        } finally {
            // devolve ao cliente o corpo que ficou guardado
            cachingResponse.copyBodyToResponse();
        }
    }
}
