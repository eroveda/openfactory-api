package io.openfactory.api.exception;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class ApiExceptionMapper {

    @ServerExceptionMapper
    public Response mapNotFoundException(NotFoundException e) {
        return error(404, "Not Found", e.getMessage());
    }

    @ServerExceptionMapper
    public Response mapIllegalStateException(IllegalStateException e) {
        return error(409, "Conflict", e.getMessage());
    }

    @ServerExceptionMapper
    public Response mapIllegalArgumentException(IllegalArgumentException e) {
        return error(400, "Bad Request", e.getMessage());
    }

    @ServerExceptionMapper
    public Response mapException(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : "Unexpected error";
        return error(500, "Internal Server Error", message);
    }

    private Response error(int status, String error, String message) {
        return Response.status(status)
            .type(MediaType.APPLICATION_JSON)
            .entity(new ErrorResponse(status, error, message))
            .build();
    }

    public record ErrorResponse(int status, String error, String message) {}
}
