package com.example.commerce.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter extends OncePerRequestFilter {
    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = headerOrGenerate(request, TRACE_HEADER);
        String correlationId = headerOrGenerate(request, CORRELATION_HEADER);
        MDC.put("traceId", traceId);
        MDC.put("correlationId", correlationId);
        response.setHeader(TRACE_HEADER, traceId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        request.setAttribute(TRACE_HEADER, traceId);
        request.setAttribute(CORRELATION_HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String headerOrGenerate(HttpServletRequest request, String header) {
        String value = request.getHeader(header);
        if (value == null || value.isBlank()) {
            value = UUID.randomUUID().toString();
        }
        return value;
    }
}
