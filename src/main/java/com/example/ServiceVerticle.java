package com.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import javax.sound.midi.SysexMessage;

/**
 * ServiceVerticle class is used generate the serviceVerticle which listens on the address "poll.data"
 * and when ever request received this corresponds to get the relevant data from the 3rd party server
 * and respond back that data to the previous "poll.data" request.
 **/
public class ServiceVerticle extends AbstractVerticle {

  public void start(Promise<Void> startPromise) {
    // create event bus object
    EventBus eventBus = vertx.eventBus();

    // consume on the "poll.data" address
    eventBus.consumer("poll.data", this::handlePollRequest);

    startPromise.complete();
  }

  // create the handler to call another http endpoint and get the data
  private void handlePollRequest(Message<?> message) {
    JsonObject receivedBody = (JsonObject) message.body();

    // Create Http client and get the data from the third party rest server
    WebClient webClient = WebClient.create(vertx);


    String path = String.format("/get/%s/", receivedBody.getString("id"));

    int PORT = Integer.parseInt(System.getenv("SERVICE_PORT"));
    String HOST = System.getenv("SERVICE_HOST");

    webClient.get(PORT, HOST, path).send()
      .onSuccess(res -> {
      String body = res.bodyAsString();

      // reply to the request with the data obtained
      message.reply(new JsonObject(res.body()));

    }).onFailure(err -> {
      System.out.println("handlePollRequest: Error" + err.getMessage());
      message.fail(500, "Internal Error");
    });
  }
}
