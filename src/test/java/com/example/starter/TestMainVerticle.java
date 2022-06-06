package com.example.starter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;

@RunWith(VertxUnitRunner.class)
public class TestMainVerticle {

  HttpClient httpClient;
  @Rule
  public RunTestOnContext rule = new RunTestOnContext();

  @Before
  public void deploy_verticle(TestContext testContext) {
    Vertx vertx = rule.vertx();
    HttpClientOptions copts = new HttpClientOptions()
				.setDefaultHost("localhost")
				.setDefaultPort(8888)
				.setSsl(true)
				.setTrustOptions(new JksOptions().setPath("server-keystore.jks").setPassword("secret"));
		httpClient = vertx.createHttpClient(copts);
    vertx.deployVerticle(new MainVerticle(), testContext.asyncAssertSuccess());
  }

  @Test
  public void verticle_deployed(TestContext testContext) throws Throwable {
    Async async = testContext.async();
    
		WebClient webclient = WebClient.wrap(httpClient);
    WebClientSession session = WebClientSession.create(webclient, new DebugCookieStore());
    DefaultCookie testCookie = new DefaultCookie("MyCookie", "MyContent");
    testCookie.setSecure(true);
    testCookie.setDomain("localhost");
    testCookie.setPath("/");                              
    session.cookieStore().put(testCookie);
    System.out.println(session.get(8888,"localhost","/").headers());
    session.get(8888,"localhost","/").send();
    async.complete();
  }
}
