package com.senac.tsi.MusicApi.web;

import com.senac.tsi.MusicApi.config.WebConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// devolve no header X-API-Version qual versao respondeu, para o cliente confirmar (sem o header na requisicao, e a 1)
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requested = request.getHeader(WebConfig.API_VERSION_HEADER);
        response.setHeader(WebConfig.API_VERSION_HEADER,
                requested != null && !requested.isBlank() ? requested.trim() : WebConfig.DEFAULT_API_VERSION);
        return true;
    }
}
