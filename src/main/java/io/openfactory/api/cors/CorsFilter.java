package io.openfactory.api.cors;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Provider
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @ConfigProperty(name = "cors.origins", defaultValue = "http://localhost:3000,http://localhost:5173")
    String allowedOriginsConfig;

    @Override
    public void filter(ContainerRequestContext req) {
        // Handle preflight OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            req.abortWith(Response.ok().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        String origin = req.getHeaderString("Origin");
        if (origin == null) return;

        Set<String> allowed = Arrays.stream(allowedOriginsConfig.split(","))
            .map(String::trim)
            .collect(Collectors.toSet());

        if (allowed.contains(origin) || allowed.contains("*")) {
            res.getHeaders().putSingle("Access-Control-Allow-Origin",      origin);
            res.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
            res.getHeaders().putSingle("Access-Control-Allow-Methods",     "GET,POST,PUT,PATCH,DELETE,OPTIONS");
            res.getHeaders().putSingle("Access-Control-Allow-Headers",     "Authorization,Content-Type,Accept");
            res.getHeaders().putSingle("Access-Control-Expose-Headers",    "Content-Disposition");
        }
    }
}
