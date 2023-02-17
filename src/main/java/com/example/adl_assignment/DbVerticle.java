package com.example.adl_assignment;

import io.github.cdimascio.dotenv.Dotenv;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

/**
 * DBVerticle is used to listen in the "database.save" address and save the data to the database
 */
public class DbVerticle extends AbstractVerticle {

  private Dotenv dotenv;

  private JDBCPool pool;

  public void start(Promise<Void> startPromise) {
    dotenv = Dotenv.configure().load();
    System.out.println("DBVerticle Deployed...");
    EventBus eventBus = vertx.eventBus();

    // configuring MYSQL client
    JsonObject config = new JsonObject()
      .put("url", dotenv.get("MYSQL_URL"))
      .put("driver_class", dotenv.get("MYSQL_DRIVER_CLASS"))
      .put("user", dotenv.get("MYSQL_USER"))
      .put("password", dotenv.get("MYSQL_PASSWORD"));

    pool = JDBCPool.pool(vertx, config);

    // create the event instance and consume on the "database.save" address
    // connect the handler to the database save method
    eventBus.consumer("database.save", this::handleDbSave);
    startPromise.complete();
  }

  private void handleDbSave(Message<Object> message) {
    JsonObject dataObj = (JsonObject) message.body();

    pool.preparedQuery("INSERT INTO studentDB.students (id, name, class)\n" +
        "VALUES (?, ?, ?)\n" +
        "ON DUPLICATE KEY UPDATE name=?, class=?;")
      .execute(Tuple.of(dataObj.getString("id"), dataObj.getString("name"), dataObj.getString("class"), dataObj.getString("name"), dataObj.getString("class")))
      .onSuccess(rows -> {
        for (Row row : rows) {
          System.out.println(row.toJson().encodePrettily());
        }
      })
      .onFailure(err -> {
        System.out.println(err.getMessage());
      });
  }
}
