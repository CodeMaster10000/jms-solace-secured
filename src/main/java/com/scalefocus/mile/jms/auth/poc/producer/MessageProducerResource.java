package com.scalefocus.mile.jms.auth.poc.producer;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("/solace")
public class MessageProducerResource {

    private final MessageProducerService messageProducerService;

    MessageProducerResource(MessageProducerService messageProducerService) {
        this.messageProducerService = messageProducerService;
    }

    @GET
    @Path("{message}")
    public Response sendMessage(@PathParam("message") String message) {
        return messageProducerService.sendMessageToBroker(message);
    }

}
