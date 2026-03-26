package io.openfactory.api.workpack;

import io.openfactory.api.auth.DevUserService;
import io.openfactory.api.user.model.User;
import io.openfactory.api.workpack.model.ProcessingStatus;
import io.openfactory.api.workpack.model.Workpack;
import io.openfactory.api.workpack.model.WorkpackStage;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class WorkpackResourceTest {

    @InjectMock
    WorkpackService workpackService;

    @InjectMock
    DevUserService devUserService;

    private User devUser;

    @BeforeEach
    void setup() {
        devUser = new User();
        devUser.id        = UUID.randomUUID();
        devUser.email     = "dev@openfactory.io";
        devUser.name      = "Dev User";
        devUser.supabaseId = "dev-user";
        when(devUserService.getOrCreateDevUser()).thenReturn(devUser);
    }

    @Test
    void listReturnsWorkpacks() {
        Workpack w = workpack("My Project", WorkpackStage.SHAPE);
        when(workpackService.listForUser(any())).thenReturn(List.of(w));

        given()
            .when().get("/api/workpacks")
            .then()
            .statusCode(200)
            .body("$.size()", is(1))
            .body("[0].title", is("My Project"));
    }

    @Test
    void listReturnsEmptyList() {
        when(workpackService.listForUser(any())).thenReturn(List.of());

        given()
            .when().get("/api/workpacks")
            .then()
            .statusCode(200)
            .body("$.size()", is(0));
    }

    @Test
    void findByIdReturns200() {
        UUID id = UUID.randomUUID();
        Workpack w = workpack("Found Pack", WorkpackStage.DEFINE);
        when(workpackService.findById(id)).thenReturn(w);

        given()
            .when().get("/api/workpacks/" + id)
            .then()
            .statusCode(200)
            .body("title", is("Found Pack"));
    }

    @Test
    void findByIdReturns404WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(workpackService.findById(id)).thenThrow(new NotFoundException("Workpack not found: " + id));

        given()
            .when().get("/api/workpacks/" + id)
            .then()
            .statusCode(404)
            .body("error", is("Not Found"))
            .body("message", containsString("Workpack not found"));
    }

    @Test
    void advanceStageReturnsUpdatedWorkpack() {
        UUID id = UUID.randomUUID();
        Workpack w = workpack("Pack", WorkpackStage.DEFINE);
        when(workpackService.advanceStage(id)).thenReturn(w);

        given()
            .contentType(ContentType.JSON)
            .when().post("/api/workpacks/" + id + "/advance")
            .then()
            .statusCode(200)
            .body("stage", is("DEFINE"));
    }

    @Test
    void advanceStageReturns409WhenAtBox() {
        UUID id = UUID.randomUUID();
        when(workpackService.advanceStage(id))
            .thenThrow(new IllegalStateException("Workpack " + id + " is already at BOX stage."));

        given()
            .contentType(ContentType.JSON)
            .when().post("/api/workpacks/" + id + "/advance")
            .then()
            .statusCode(409)
            .body("error", is("Conflict"));
    }

    @Test
    void updateTitleReturnsUpdatedWorkpack() {
        UUID id = UUID.randomUUID();
        Workpack w = workpack("New Title", WorkpackStage.RAW);
        when(workpackService.updateTitle(eq(id), eq("New Title"))).thenReturn(w);

        given()
            .contentType(ContentType.JSON)
            .body("{\"title\": \"New Title\"}")
            .when().patch("/api/workpacks/" + id + "/title")
            .then()
            .statusCode(200)
            .body("title", is("New Title"));
    }

    // -----------------------------------------------------------------------
    // Async ingest
    // -----------------------------------------------------------------------

    @Test
    void ingestReturnsProcessingStatus() {
        Workpack w = workpack("New Pack", WorkpackStage.RAW);
        w.processingStatus = ProcessingStatus.PROCESSING;
        when(workpackService.ingest(eq("New Pack"), any(), eq("Some content"))).thenReturn(w);

        given()
            .contentType(ContentType.JSON)
            .body("{\"title\": \"New Pack\", \"content\": \"Some content\"}")
            .when().post("/api/workpacks/ingest")
            .then()
            .statusCode(200)
            .body("processingStatus", is("PROCESSING"));
    }

    @Test
    void ingestReturns400WhenTitleBlank() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"title\": \"\", \"content\": \"Some content\"}")
            .when().post("/api/workpacks/ingest")
            .then()
            .statusCode(400)
            .body("error", is("Bad Request"))
            .body("message", containsString("title is required"));
    }

    @Test
    void ingestReturns400WhenContentBlank() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"title\": \"My Pack\", \"content\": \"\"}")
            .when().post("/api/workpacks/ingest")
            .then()
            .statusCode(400)
            .body("error", is("Bad Request"))
            .body("message", containsString("content is required"));
    }

    @Test
    void ingestReturns400WhenTitleTooLong() {
        String longTitle = "a".repeat(501);
        given()
            .contentType(ContentType.JSON)
            .body("{\"title\": \"" + longTitle + "\", \"content\": \"Some content\"}")
            .when().post("/api/workpacks/ingest")
            .then()
            .statusCode(400)
            .body("error", is("Bad Request"))
            .body("message", containsString("500 characters"));
    }

    @Test
    void ingestReturns400WhenContentTooLong() {
        String longContent = "a".repeat(50_001);
        given()
            .contentType(ContentType.JSON)
            .body("{\"title\": \"My Pack\", \"content\": \"" + longContent + "\"}")
            .when().post("/api/workpacks/ingest")
            .then()
            .statusCode(400)
            .body("error", is("Bad Request"))
            .body("message", containsString("50,000 characters"));
    }

    @Test
    void shapeReturnsProcessingStatus() {
        UUID id = UUID.randomUUID();
        Workpack w = workpack("Pack", WorkpackStage.SHAPE);
        w.processingStatus = ProcessingStatus.PROCESSING;
        when(workpackService.reshape(id)).thenReturn(w);

        given()
            .contentType(ContentType.JSON)
            .when().post("/api/workpacks/" + id + "/shape")
            .then()
            .statusCode(200)
            .body("processingStatus", is("PROCESSING"));
    }

    // -----------------------------------------------------------------------

    private Workpack workpack(String title, WorkpackStage stage) {
        Workpack w = new Workpack();
        w.id    = UUID.randomUUID();
        w.title = title;
        w.stage = stage;
        w.owner = devUser;
        return w;
    }
}
