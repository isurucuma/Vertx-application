package com.example.adl_assignment;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * MainVerticle is deploying 2 other verticles (DbVerticle and ServiceVerticle) and also provides rest
 * endpoint on localhost:5000/resource/{id}/ and reply back with the response
 */
public class MainVerticle extends AbstractVerticle {

  private HttpServer httpServer;
  private EventBus eventBus;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    // deploy DbVerticle
    vertx.deployVerticle(new DbVerticle());

    // deploy ServiceVerticle
    vertx.deployVerticle(new ServiceVerticle());

    // create the http endpoint listen on the "localhost/request/{id}"
    httpServer = vertx.createHttpServer();
    eventBus = vertx.eventBus();

    Router router = Router.router(this.vertx);
    router.route(HttpMethod.GET, "/resource/:id/").handler(this::handleResourceRequest);
    httpServer.requestHandler(router).listen(5000, ar -> {
      if (ar.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP MainServer is listening on port 5000");
      } else {
        startPromise.fail(ar.cause());
      }
    });
  }

  private void handleResourceRequest(RoutingContext rc) {
    System.out.println("HandleResourceRequest: Request received...");
    // create the request handler
    // get the id from the request
    String id = rc.pathParam("id");
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("id", id);

    try{
      // send request to "poll.data" address
      eventBus.request("poll.data", jsonObject, ar -> {
        handlePollData(ar, rc);
      });
    }
    catch(Exception e){
      System.err.println(e.getMessage());
      rc.fail(400);
    }
  }

  private void handlePollData(AsyncResult<Message<Object>> asyncResult, RoutingContext rc) {

    try{
      // take the data from the response
      if (asyncResult.succeeded()) {
        Message<Object> message = asyncResult.result();
        JsonObject dataObj = (JsonObject) message.body();
        System.out.println("This is the received message: " + dataObj.encodePrettily());

        // pass data to the DBVerticle to save to the database
        eventBus.send("database.save", dataObj);
        // send response to the http
        rc.end(dataObj.encodePrettily());


      } else {
        // It has failed
        System.out.println("handle poll data failed...");
        rc.fail(400);
      }
    }
    catch(Exception e){
      System.err.println(e.getMessage());
      rc.fail(400);
    }
  }
}
