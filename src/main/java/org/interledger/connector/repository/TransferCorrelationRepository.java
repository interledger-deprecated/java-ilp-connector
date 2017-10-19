package org.interledger.connector.repository;

import org.interledger.plugin.lpi.TransferId;

import java.util.Optional;

/**
 * Defines how an instance of {@link TransferCorrelation} can be loaded and saved to a persistent
 * data repository.
 */
public interface TransferCorrelationRepository {

  void save(TransferCorrelation transferCorrelation);

  Optional<TransferCorrelation> findByDestinationTransferId(TransferId destinationTransferId);

}
