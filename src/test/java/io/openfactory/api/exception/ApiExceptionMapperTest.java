package io.openfactory.api.exception;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionMapperTest {

    private final ApiExceptionMapper mapper = new ApiExceptionMapper();

    @Test
    void notFoundMaps404() {
        Response r = mapper.mapNotFoundException(new NotFoundException("Workpack not found: abc"));
        assertEquals(404, r.getStatus());
        ApiExceptionMapper.ErrorResponse body = (ApiExceptionMapper.ErrorResponse) r.getEntity();
        assertEquals("Not Found", body.error());
        assertEquals("Workpack not found: abc", body.message());
    }

    @Test
    void illegalStateMaps409() {
        Response r = mapper.mapIllegalStateException(new IllegalStateException("Already a member"));
        assertEquals(409, r.getStatus());
        ApiExceptionMapper.ErrorResponse body = (ApiExceptionMapper.ErrorResponse) r.getEntity();
        assertEquals("Conflict", body.error());
    }

    @Test
    void illegalArgumentMaps400() {
        Response r = mapper.mapIllegalArgumentException(new IllegalArgumentException("Invalid input"));
        assertEquals(400, r.getStatus());
        ApiExceptionMapper.ErrorResponse body = (ApiExceptionMapper.ErrorResponse) r.getEntity();
        assertEquals("Bad Request", body.error());
    }

    @Test
    void genericExceptionMaps500() {
        Response r = mapper.mapException(new RuntimeException("Something exploded"));
        assertEquals(500, r.getStatus());
        ApiExceptionMapper.ErrorResponse body = (ApiExceptionMapper.ErrorResponse) r.getEntity();
        assertEquals("Internal Server Error", body.error());
        assertEquals("Something exploded", body.message());
    }

    @Test
    void genericExceptionWithNullMessageReturnsDefault() {
        Response r = mapper.mapException(new RuntimeException());
        assertEquals(500, r.getStatus());
        ApiExceptionMapper.ErrorResponse body = (ApiExceptionMapper.ErrorResponse) r.getEntity();
        assertEquals("Unexpected error", body.message());
    }
}
