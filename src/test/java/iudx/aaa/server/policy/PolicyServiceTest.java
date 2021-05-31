package iudx.aaa.server.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.apache.logging.log4j.Logger;
import iudx.aaa.server.configuration.Configuration;
import iudx.aaa.server.postgres.client.PostgresClient;
import java.util.HashSet;
import java.util.Set;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class PolicyServiceTest {
  private static Logger LOGGER = LogManager.getLogger(PolicyServiceTest.class);

  private static Configuration config;

  /* Database Properties */
  private static String databaseIP;
  private static int databasePort;
  private static String databaseName;
  private static String databaseUserName;
  private static String databasePassword;
  private static int poolSize;
  private static PgPool pgclient;
  private static PoolOptions poolOptions;
  private static PgConnectOptions connectOptions;
  private static PostgresClient pgClient;
  private static PolicyService policyService;
  private static Vertx vertxObj;
  private static WebMockCatalogueClient catMock;
  private static WebClient client;
  private static Item item;
  
  @InjectMocks
  private static WebMockCatalogueClient fetchItem;
  
  @Captor
  ArgumentCaptor<Item> item2;
  
  
  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, io.vertx.reactivex.core.Vertx vertx2,
      VertxTestContext testContext) {
    config = new Configuration();
    vertxObj = vertx;
    JsonObject dbConfig = config.configLoader(1, vertx2);

    /* Read the configuration and set the postgres client properties. */
    LOGGER.debug("Info : Reading config file");

    databaseIP = dbConfig.getString("databaseIP");
    databasePort = Integer.parseInt(dbConfig.getString("databasePort"));
    databaseName = dbConfig.getString("databaseName");
    databaseUserName = dbConfig.getString("databaseUserName");
    databasePassword = dbConfig.getString("databasePassword");
    poolSize = Integer.parseInt(dbConfig.getString("poolSize"));

    /* Set Connection Object */
    if (connectOptions == null) {
      connectOptions = new PgConnectOptions().setPort(databasePort).setHost(databaseIP)
          .setDatabase(databaseName).setUser(databaseUserName).setPassword(databasePassword);
    }

    /* Pool options */
    if (poolOptions == null) {
      poolOptions = new PoolOptions().setMaxSize(poolSize);
    }

    /* Create the client pool */
    pgclient = PgPool.pool(vertx, connectOptions, poolOptions);

    pgClient = new PostgresClient(vertx, connectOptions, poolOptions);

    policyService = new PolicyServiceImpl(pgClient);
    
    catMock = new WebMockCatalogueClient();
    Set<String> servers = new HashSet<>();
    
    item = new Item();
    item.setId("datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo/PuneRailwayStation_28");
    item.setProviderID("datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc");
    item.setType("iudx:Resource");
    item.setServers(servers);

    testContext.completeNow();

    
  }

  @AfterEach
  public void finish(VertxTestContext testContext) {
    LOGGER.info("Finishing....");
    vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  }

  @Test
  @DisplayName("Testing Successful policy creation")
  void createPolicySuccess(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("email", "email");

    policyService.createPolicy(request,
        testContext.succeeding(response -> testContext.verify(() -> {
          assertEquals("success", response.getString("status"));
          testContext.completeNow();
        })));
  }

  @Test
  @DisplayName("Testing Failure in  policy creation")
  void createPolicyFailure(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("email", "email");

    policyService.createPolicy(request,
        testContext.succeeding(response -> testContext.verify(() -> {
          assertEquals("failed", response.getString("status"));
          testContext.completeNow();
        })));
  }
}
