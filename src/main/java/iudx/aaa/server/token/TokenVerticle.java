package iudx.aaa.server.token;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;
import iudx.aaa.server.policy.PolicyService;
import iudx.aaa.server.postgres.client.PostgresClient;
import static iudx.aaa.server.token.Constants.*;

/**
 * The Token Verticle.
 * <h1>Token Verticle</h1>
 * <p>
 * The Token Verticle implementation in the the IUDX AAA Server exposes the
 * {@link iudx.aaa.server.token.TokenService} over the Vert.x Event Bus.
 * </p>
 *
 * @version 1.0
 * @since 2020-12-15
 */

public class TokenVerticle extends AbstractVerticle {

  /* Database Properties */
  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int poolSize;
  private PoolOptions poolOptions;
  private PgConnectOptions connectOptions;
  private PostgresClient pgClient;
  private TokenService tokenService;  
  private String keystorePath;
  private String keystorePassword;
  private JWTAuth provider;
  private PolicyService policyService;
  private HttpWebClient httpWebClient;
  
  private static final Logger LOGGER = LogManager.getLogger(TokenVerticle.class);

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   */

  @Override
  public void start() throws Exception {

    /* Read the configuration and set the postgres client properties. */
    LOGGER.debug("Info : " + LOGGER.getName() + " : Reading config file");
    databaseIP = config().getString("databaseIP");
    databasePort = Integer.parseInt(config().getString("databasePort"));
    databaseName = config().getString("databaseName");
    databaseUserName = config().getString("databaseUserName");
    databasePassword = config().getString("databasePassword");
    poolSize = Integer.parseInt(config().getString("poolSize"));
    keystorePath = config().getString("keystorePath");
    keystorePassword = config().getString("keystorePassword");
    String issuer = config().getString("authServerDomain","");
    JsonObject keycloakOptions = config().getJsonObject("keycloakOptions");
    
    if(issuer != null && !issuer.isBlank()) {
      CLAIM_ISSUER = issuer;
    } else {
      LOGGER.fatal("Fail: authServerDomain not set");
      throw new IllegalStateException("authServerDomain not set");
    }

    /* Set Connection Object */
    if (connectOptions == null) {
      connectOptions = new PgConnectOptions().setPort(databasePort).setHost(databaseIP)
          .setDatabase(databaseName).setUser(databaseUserName).setPassword(databasePassword)
          .setConnectTimeout(PG_CONNECTION_TIMEOUT);
    }

    /* Pool options */
    if (poolOptions == null) {
      poolOptions = new PoolOptions().setMaxSize(poolSize);
    }
        
    /* Initializing the services */
    provider = jwtInitConfig();
    httpWebClient = new HttpWebClient(vertx, keycloakOptions);
    pgClient = new PostgresClient(vertx, connectOptions, poolOptions);
    policyService = PolicyService.createProxy(vertx, POLICY_SERVICE_ADDRESS);
    tokenService = new TokenServiceImpl(pgClient, policyService, provider, httpWebClient);
    
    new ServiceBinder(vertx).setAddress(TOKEN_SERVICE_ADDRESS).register(TokenService.class,
        tokenService);

    LOGGER.debug("Info : " + LOGGER.getName() + " : Started");
  }
  
  /**
   * Initializes {@link JWTAuth} to create a Authentication Provider instance for JWT token.
   * Authentication Provider is used to generate and authenticate JWT token. 
   * @return provider
   */
  public JWTAuth jwtInitConfig() {
    JWTAuthOptions config = new JWTAuthOptions();
    config.setKeyStore(
        new KeyStoreOptions()
          .setPath(keystorePath)
          .setPassword(keystorePassword));

    JWTAuth provider = JWTAuth.create(vertx, config);
    return provider;
  }
}
