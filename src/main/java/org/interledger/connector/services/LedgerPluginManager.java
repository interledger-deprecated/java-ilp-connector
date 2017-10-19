package org.interledger.connector.services;

import org.interledger.InterledgerAddress;
import org.interledger.connector.repository.TransferCorrelationRepository;
import org.interledger.connector.subprotocols.SubprotocolDataService;
import org.interledger.plugin.lpi.LedgerPlugin;
import org.interledger.plugin.lpi.LedgerPluginConfig;
import org.interledger.plugin.lpi.TransferId;

import org.immutables.value.Value.Default;

import java.util.Objects;
import java.util.Optional;

/**
 * A centralized service that manages all of a Connector's {@link LedgerPlugin} instances for a
 * given connector Interledger address. This service manages functionality relating to plugin
 * startup, graceful shutdown, and more.
 *
 * Note that event listeners are not managed by this manager, but are instead registered by external
 * callers, and react to error by potentially removing themselves as listeners.
 */
public interface LedgerPluginManager {

  /**
   * Add a new plugin to this manager for the given {@code ledgerPrefix} and {@code tradingPair}.
   *
   * @param ledgerPluginConfig A {@link LedgerPluginConfig} with configuration information to apply
   *                           to the plugin.
   * @param ledgerPlugin       A {@link LedgerPlugin} corresponding to the supplied ledger prefix.
   */
  void addLedgerPlugin(LedgerPluginConfig ledgerPluginConfig, LedgerPlugin ledgerPlugin);

  /**
   * Add a new plugin to this manager for the given {@code ledgerPrefix} and {@code tradingPair}.
   *
   * @param ledgerPrefix A {@link InterledgerAddress} corresponding to the supplied ledger plugin to
   *                     remove.
   */
  void removeLedgerPlugin(InterledgerAddress ledgerPrefix);

  /**
   * Get the Ledger Plugin for the specified {@code ledgerPrefix}.
   *
   * @param ledgerPrefix A {@link InterledgerAddress} for the prefix of the LedgerPlugin to manage.
   *
   * @return The requested {@link LedgerPlugin}, if available.
   */
  Optional<LedgerPlugin> getLedgerPlugin(InterledgerAddress ledgerPrefix);

  /**
   * An accessor method for implementations to return a Ledger Plugin for the specified {@code
   * ledgerPrefix}, assuming that it should be present.
   *
   * This method acts as a kind of precondition check for the presence of a ledger plugin because,
   * if this method fails, it's probable that the plugin being accessed might actually be the plugin
   * we would send an ILP rejection message back upon, which would be impossible if the plugin were
   * missing. Thus, there is likely no recovery path for this type of exception exists except for
   * the connector to perhaps retry the operation in the future.
   *
   * @param transferId   The {@link TransferId} of the transfer the plugin was requested for.
   * @param ledgerPrefix A {@link InterledgerAddress} for the prefix of the LedgerPlugin to manage.
   *
   * @return The requested {@link LedgerPlugin}, if available.
   *
   * @throws RuntimeException if the ledger plugin is not present in this manager.
   */
  @Default
  default LedgerPlugin getLedgerPluginSafe(
      final TransferId transferId, final InterledgerAddress ledgerPrefix
  ) {
    Objects.requireNonNull(ledgerPrefix);
    InterledgerAddress.requireLedgerPrefix(ledgerPrefix);
    Objects.requireNonNull(transferId);

    return this.getLedgerPlugin(ledgerPrefix).orElseThrow(() -> new RuntimeException(
        String.format(
            "For TransferId '%s', LedgerPlugin '%s' was not currently connected to this connector!",
            transferId, ledgerPrefix
        )));
  }

  TransferCorrelationRepository getTransferCorrelationRepository();

  SubprotocolDataService getSubprotocolDataService();
}
