package org.interledger.connector.repository;


import org.interledger.plugin.lpi.Transfer;
import org.interledger.plugin.lpi.TransferId;

import org.immutables.value.Value;
import org.immutables.value.Value.Default;

/**
 * Defines a data structure that can be used to correlate a source {@link Transfer} with a
 * destination {@link Transfer} on another ledger.
 *
 * In general, a source-transfer will be created for this connector on a given ledger, which means
 * value is "on hold" for this connector, which merely has to make a second transfer is a
 * destination ledger in order to move an Interledger payment along towards completion. Once the
 * ultimate Interledger payment is fulfilled, this mechanism can be used to determine one transfer
 * when only a correlating transfer identifier is available.
 *
 * An alternative to this service would be to encode information about a source-transfer inside of
 * the sub-protocol portion of a destination transfer. While acceptable, this leaks information
 * about the source transfer into the destination ledger, which may not be desirable.
 */
@Value.Immutable
public interface TransferCorrelation {

  @Default
  default TransferId getSourceTransferId() {
    return this.getSourceTransfer().getTransferId();
  }

  @Default
  default TransferId getDestinationTransferId() {
    return this.getDestinationTransfer().getTransferId();
  }

  Transfer getSourceTransfer();

  Transfer getDestinationTransfer();

}
