package org.interledger.connector.repository;

import org.interledger.plugin.lpi.TransferId;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An implementation of {@link TransferCorrelationRepository} that stores correlations in-memory in
 * a non-durable fashion.
 *
 * This implementation is meant for light-weight Connectors or for demonstration/testing purposes.
 */
public class InMemoryTransferCorrelationRepository implements TransferCorrelationRepository {

  private final Map<TransferId, TransferCorrelation> correlatedTransfer;

  public InMemoryTransferCorrelationRepository() {
    this.correlatedTransfer = Maps.newConcurrentMap();
  }

  @Override
  public void save(TransferCorrelation transferCorrelation) {
    this.correlatedTransfer
        .put(transferCorrelation.getDestinationTransferId(), transferCorrelation);
  }

  @Override
  public Optional<TransferCorrelation> findByDestinationTransferId(
      final TransferId destinationTransferId
  ) {
    Objects.requireNonNull(destinationTransferId);
    return Optional.ofNullable(correlatedTransfer.get(destinationTransferId));
  }
}
