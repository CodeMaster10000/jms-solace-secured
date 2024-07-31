package com.scalefocus.mile.jms.auth.poc.producer;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

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
