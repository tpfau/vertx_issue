package com.example.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    
    JksOptions keyOptions = new JksOptions()
    .setPath("server-keystore.jks")
    .setPassword("secret");		 

    HttpServerOptions opts = new HttpServerOptions()
									 .setLogActivity(true) 
									 .setSsl(true)
									 .setKeyStoreOptions(keyOptions);
    
    vertx.createHttpServer(opts).requestHandler(req -> {
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x!");
    }).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
}
