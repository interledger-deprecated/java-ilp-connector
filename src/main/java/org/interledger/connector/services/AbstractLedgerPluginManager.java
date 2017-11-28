package org.interledger.connector.services;

import org.interledger.InterledgerAddress;
import org.interledger.connector.repository.TransferCorrelationRepository;
import org.interledger.connector.subprotocols.SubprotocolDataService;
import org.interledger.plugin.lpi.LedgerPlugin;
import org.interledger.plugin.lpi.LedgerPluginConfig;
import org.interledger.plugin.lpi.events.ImmutableLedgerPluginErrorEvent;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An abstract implementatino of {@link LedgerPluginManager}.
 */
public abstract class AbstractLedgerPluginManager implements LedgerPluginManager {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final SubprotocolDataService subprotocolDataService;
  private final TransferCorrelationRepository transferCorrelationRepository;
  private final Map<InterledgerAddress, LedgerPlugin> ledgerPluginMap;

  public AbstractLedgerPluginManager(
    final SubprotocolDataService subprotocolDataService,
    final TransferCorrelationRepository transferCorrelationRepository
  ) {
    this.subprotocolDataService = Objects.requireNonNull(subprotocolDataService);
    this.transferCorrelationRepository = Objects.requireNonNull(transferCorrelationRepository);
    this.ledgerPluginMap = Maps.newConcurrentMap();
  }

  @Override
  public void addLedgerPlugin(
    final LedgerPluginConfig ledgerPluginConfig, final LedgerPlugin ledgerPlugin
  ) {
    Objects.requireNonNull(ledgerPluginConfig);
    Objects.requireNonNull(ledgerPlugin);

    // If the ledger plugin is present, we should disconnect it, and reconnect with the new plugin.
    final InterledgerAddress ledgerPrefix = ledgerPluginConfig.getLedgerPrefix();
    this.getLedgerPlugin(ledgerPrefix).ifPresent(LedgerPlugin::disconnect);

    try {
      // Connect to the Ledger via the associated LedgerPlugin...
      ledgerPlugin.connect();

      // ...and then add to the plugin map. Adding the Connector to this map actually enables it
      // from the perspective of the Connector.
      this.ledgerPluginMap.put(ledgerPrefix, ledgerPlugin);
    } catch (Exception e) {
      logger.error("LedgerPlugin failed to connect. Error: {}", e.getMessage(), e);
      // emit an error to the ledger plugin...
      ledgerPlugin.getLedgerPluginEventEmitter().emitEvent(
        ImmutableLedgerPluginErrorEvent.builder()
          .ledgerPrefix(ledgerPluginConfig.getLedgerPrefix())
          .error(e)
          .build()
      );
    }
  }

  @Override
  public void removeLedgerPlugin(final InterledgerAddress ledgerPrefix) {
    Objects.requireNonNull(ledgerPrefix);

    // Disconnect from the Ledger via the associated LedgerPlugin.
    Optional.ofNullable(this.ledgerPluginMap.remove(ledgerPrefix))
      .ifPresent(LedgerPlugin::disconnect);
  }

  @Override
  public Optional<LedgerPlugin> getLedgerPlugin(final InterledgerAddress ledgerPrefix) {
    Objects.requireNonNull(ledgerPrefix);
    InterledgerAddress.requireAddressPrefix(ledgerPrefix);

    return Optional.ofNullable(ledgerPluginMap.get(ledgerPrefix));
  }

//    /**
//     * Get all ledger plugins.
//     */
//    @Override
//    public Collection<LedgerPlugin> getLedgerPlugins() {
//      return this.ledgerPluginMap.values();
//    }

  @Override
  public TransferCorrelationRepository getTransferCorrelationRepository() {
    return this.transferCorrelationRepository;
  }

  @Override
  public SubprotocolDataService getSubprotocolDataService() {
    return this.subprotocolDataService;
  }
}
