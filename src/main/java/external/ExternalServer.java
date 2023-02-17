package external;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

/**
 * ExternalServer is used to mimic a 3rd party service to get the student details by sending the ID
 * to the rest endpoint localhost:8080/get/{id}/
 * Run this separately
 */
public class ExternalServer extends AbstractVerticle {

  private HttpServer httpServer;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new ExternalServer());
  }

  public void start(Promise<Void> startPromise) {
    httpServer = vertx.createHttpServer();

    Router router = Router.router(vertx);
    router.route(HttpMethod.GET, "/get/:id/").handler(ctx -> {
      String id = ctx.pathParam("id");
      ctx.response().putHeader("Content-Type", "application/json");
      JsonObject responseObj = new JsonObject();
      responseObj.put("id", id);
      responseObj.put("name", "AAA-".concat(id));
      responseObj.put("class", "UpperClass-".concat(id));

      ctx.response().end(responseObj.encodePrettily());
    });

    httpServer.requestHandler(router).listen(8080, asyncresult -> {
      if (asyncresult.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP ExternalServer is listening on port 8080");

      } else {
        startPromise.fail(asyncresult.cause());
      }
    });
  }
}
