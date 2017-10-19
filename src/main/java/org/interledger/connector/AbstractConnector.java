package org.interledger.connector;

import org.interledger.connector.config.ConnectorConfigurationService;
import org.interledger.connector.services.LedgerPluginManager;
import org.interledger.plugin.lpi.LedgerPlugin;
import org.interledger.plugin.lpi.LedgerPluginConfig;
import org.interledger.plugin.lpi.handlers.LedgerPluginEventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * An abstract implementation of a {@link Connector}.
 */
public abstract class AbstractConnector<T extends ConnectorConfigurationService> implements
  Connector<T> {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final LedgerPluginManager ledgerPluginManager;

  // Only one handler for the connector handles events.
  private final LedgerPluginEventHandler ledgerPluginEventHandler;

  private final T connectorConfigService;

  public AbstractConnector(
    final LedgerPluginManager ledgerPluginManager,
    final LedgerPluginEventHandler ledgerPluginEventHandler,
    T connectorConfigService
  ) {
    this.ledgerPluginManager = Objects.requireNonNull(ledgerPluginManager);
    this.ledgerPluginEventHandler = Objects.requireNonNull(ledgerPluginEventHandler);
    this.connectorConfigService = Objects.requireNonNull(connectorConfigService);

    this.initializeConnector();
  }

  /**
   * Initializes the connector from its configuration service.
   */
  public void initializeConnector() {
    // Convert the Stream to the proper type...
    // ...then for all configured ledger-plugins, load each one and add to the LedgerPluginManager...
    final Stream<LedgerPluginConfig> ledgerPluginConfigStream = Stream
      .of(getConnectorConfigurationService().getLedgerPluginConfigurations())
      .flatMap(collection -> collection.stream());
    ledgerPluginConfigStream
      .filter(LedgerPluginConfig.class::isInstance)
      .map(LedgerPluginConfig.class::cast)
      .forEach(
        ledgerPluginConfig -> {
          try {
            final LedgerPlugin ledgerPlugin = this.constructLedgerPlugin(ledgerPluginConfig);
            ledgerPlugin.addLedgerPluginEventHandler(this.ledgerPluginEventHandler);
            this.ledgerPluginManager.addLedgerPlugin(ledgerPluginConfig, ledgerPlugin);
          } catch (Exception e) {
            logger.error("Unable to initialize LedgerPlugin: {}!", ledgerPluginConfig);
          }
        }
      );
  }

  /**
   * An abstract method that allows sub-class implementations to construct an instance of {@link LedgerPlugin} from the
   * supplied {@link LedgerPluginConfig}.
   *
   * @param ledgerPluginConfig An instance of {@link LedgerPluginConfig} used to configure a ledger-plugin.
   *
   * @return A newly constructed {@link LedgerPlugin}.
   */
  protected abstract LedgerPlugin constructLedgerPlugin(LedgerPluginConfig ledgerPluginConfig);

  @Override
  public LedgerPluginManager getLedgerPluginManager() {
    return ledgerPluginManager;
  }

  @Override
  public T getConnectorConfigurationService() {
    return this.connectorConfigService;
  }
}
