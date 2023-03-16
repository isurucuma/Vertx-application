package com.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;


/**
 * DBVerticle is used to listen in the "database.save" address and save the data to the database
 */
public class DbVerticle extends AbstractVerticle {

//  private Dotenv dotenv;

  private JDBCPool pool;

  public void start(Promise<Void> startPromise) {
//    dotenv = Dotenv.configure().load();
    EventBus eventBus = vertx.eventBus();


    // configuring MYSQL client
    JsonObject config = new JsonObject()
      .put("url", System.getenv("MYSQL_URL"))
      .put("driver_class", "com.mysql.cj.jdbc.Driver")
      .put("user", "root")
      .put("password", System.getenv("MYSQL_PASSWORD"));



    pool = JDBCPool.pool(vertx, config);

    //create the databse if not exist

    initializeDB().onSuccess(res -> {
      System.out.println("Database preparation Done");
      // create the event instance and consume on the "database.save" address
      // connect the handler to the database save method
      eventBus.consumer("database.save", this::handleDbSave);

      startPromise.complete();
    }).onFailure(err -> {
      System.err.println(err.getMessage());
      startPromise.fail("Database preparation failed...");
    });
  }

  private Future<RowSet<Row>> initializeDB() {
    Future<RowSet<Row>> query_1 = pool.query("CREATE SCHEMA IF NOT EXISTS studentDB;")
      .execute().andThen(res1 -> {
      pool.query("USE studentDB;")
        .execute().andThen(res2 -> {
          pool.query("CREATE TABLE IF NOT EXISTS studentDB.students (id varchar(255), name varchar(255), class varchar(255));").execute()
            .andThen(res3 -> {});
        });
    });
    return query_1;
  }

  private void handleDbSave(Message<Object> message) {
    JsonObject dataObj = (JsonObject) message.body();



    pool.preparedQuery("INSERT INTO studentDB.students (id, name, class)\n" +
        "VALUES (?, ?, ?)\n" +
        "ON DUPLICATE KEY UPDATE name=?, class=?;")
      .execute(Tuple.of(dataObj.getString("id"), dataObj.getString("name"), dataObj.getString("class"), dataObj.getString("name"), dataObj.getString("class")))
      .onSuccess(rows -> {
        System.out.println("Database operation completed...");
        for (Row row : rows) {
          System.out.println(row.toJson().encodePrettily());
        }
      })
      .onFailure(err -> {
        System.out.println(err.getMessage());
      });
  }
}
