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


}
