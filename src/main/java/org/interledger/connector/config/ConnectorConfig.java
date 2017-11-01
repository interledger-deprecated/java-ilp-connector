package org.interledger.connector.config;

/**
 * Defines Connector-wide configuration properties that affect the connector as a whole.
 */
public interface ConnectorConfig {

  /**
   * The minimum time, in seconds, that the connector wants to budget for getting a message to the ledgers its trading
   * on.
   *
   * Defaults to 1 second.
   */
  default Integer getMinimumMessageWindow() {
    return 1;
  }

  default Integer getMaxHoldTime() {return 10;}           // seconds

  default Double getFxSpread() {return 0.002;}

  default Double getSlippage() {return 0.001;}

  default Integer getRouteBroadcastInterval() {return 30 * 1000;}         // milliseconds

  default Integer getRouteCleanuoInterval() {return 1000;}            // milliseconds

  default Integer getRouteExpiry() {return 45 * 1000;}            // milliseconds

    default Integer getQuoteExpiry() {return 45 * 1000;}            // milliseconds


    //public HashMap<String, ?> generateDefaultPairs(? ledgers);

    //public ? parseCredentials();

    public HashMap<String, ?> parseCredentialsEnv();

    public HashMap<String, ?> parseLedgers();

    public List<Route> parseRoutes();

    public HashMap<String, ?> getLocalConfig();

    //public ? loadConnectorConfig();
}
