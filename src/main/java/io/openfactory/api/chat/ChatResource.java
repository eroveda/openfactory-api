package io.openfactory.api.chat;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

@Path("/api/workpacks/{id}/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatResource {

    @Inject
    ChatService chatService;

    @POST
    public ChatService.ChatReply chat(
            @PathParam("id") UUID workpackId,
            ChatRequest request) throws Exception {
        if (request == null || request.message() == null || request.message().isBlank())
            throw new BadRequestException("message is required");
        return chatService.chat(workpackId, request.message());
    }

    @DELETE
    public void clearHistory(@PathParam("id") UUID workpackId) {
        chatService.clearHistory(workpackId);
    }

    public record ChatRequest(String message) {}
}
