package org.interledger.connector.config;

import org.interledger.InterledgerAddress;
import org.interledger.connector.services.LedgerPluginManager;
import org.interledger.plugin.lpi.LedgerPluginConfig;

import java.util.Collection;

/**
 * A configuration service that provides typed (and runtime-reloadable) access to important configuration properties for
 * _this_ connector.
 */
public interface ConnectorConfigurationService<T extends ConnectorConfig> {

  /**
   * Accessor for Connector-wide configuration values.
   * @return Generic Type T of {@link ConnectorConfig}
   */
  T getConnectorConfig();

  /**
   * Accessor for all currently configured ledger plugins.
   *
   * Note that this list does not necessarily reflect the current status of a connection to any underlying ledger, but
   * instead merely represents information about the current configuration (whereas the current state of the connector,
   * such as the availability of a given ledger plugin due to downtime) is managed by a separate service.
   *
   * @return A {@link Collection} of type {@link LedgerPluginConfig} for all configured ledger plugins.
   */
  Collection<? extends LedgerPluginConfig> getLedgerPluginConfigurations();

  /**
   * Return configuration info for the ledger plugin indicated by the supplied {@code ledgerPrefix}. This interface
   * returns only some typed configuration properties, with any other properties contained in {@link
   * LedgerPluginConfig#getOptions()}. Implementations can write adaptor classes to access these properties in a typed
   * fashion.
   *
   * Note that this list does not necessarily reflect the current status of a connection to any underlying ledger, but
   * instead merely represents information about the current configuration (whereas the current state of the connector,
   * such as the availability of a given ledger plugin due to downtime) is managed by the {@link LedgerPluginManager}
   * and the plugin itself.
   *
   * @param ledgerPrefix An {@link InterledgerAddress} that is a ledger-prefix for a ledger that this connector has an
   *                     account on.
   *
   * @return A {@link LedgerPluginConfig} for the specified ledger prefix.
   *
   * @throws RuntimeException if ledger-plugin configuration cannot be found or otherwise assembled.
   */
  LedgerPluginConfig getLedgerPluginConfiguration(InterledgerAddress ledgerPrefix);

}
