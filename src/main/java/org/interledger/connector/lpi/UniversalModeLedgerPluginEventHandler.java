package org.interledger.connector.lpi;

import org.interledger.connector.config.ConnectorConfig;
import org.interledger.connector.config.ConnectorConfigurationService;
import org.interledger.connector.repository.TransferCorrelation;
import org.interledger.connector.routing.PaymentRouter;
import org.interledger.connector.fx.FxEngine;
import org.interledger.connector.services.LedgerPluginManager;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.plugin.lpi.LedgerPlugin;
import org.interledger.plugin.lpi.Transfer;
import org.interledger.plugin.lpi.events.IncomingMessgeRequestEvent;
import org.interledger.plugin.lpi.events.LedgerInfoChangedEvent;
import org.interledger.plugin.lpi.events.LedgerPluginConnectedEvent;
import org.interledger.plugin.lpi.events.LedgerPluginDisconnectedEvent;
import org.interledger.plugin.lpi.events.OutgoingMessgeRequestEvent;
import org.interledger.plugin.lpi.events.OutgoingTransferFulfilledEvent;
import org.interledger.plugin.lpi.handlers.LedgerPluginEventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * An implementation of {@link LedgerPluginEventHandler} that handles events from Ledger plugins running in a Connector,
 * using ILP Universal-mode behavior.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0001-interledger-architecture/0001-interledger-architecture.md"
 * @see "https://github.com/interledgerjs/ilp-connector"
 */
public class UniversalModeLedgerPluginEventHandler extends AbstractLedgerPluginEventHandler<ConnectorConfig>
    implements LedgerPluginEventHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public UniversalModeLedgerPluginEventHandler(
      final String deterministicIdSecret,
      final ConnectorConfigurationService<ConnectorConfig> connectorConfigurationService,
      final LedgerPluginManager ledgerPluginManager,
      final PaymentRouter paymentRouter,
      final FxEngine fxEngine
  ) {
    super(
        deterministicIdSecret, connectorConfigurationService, ledgerPluginManager, paymentRouter, fxEngine
    );
  }

  @Override
  public void onLedgerInfoChanged(LedgerInfoChangedEvent event) {
    logger.info("LedgerPlugin LedgerInfo Changed: {}", event);
  }

  /**
   * Called to handle an {@link LedgerPluginConnectedEvent}.
   *
   * @param event A {@link LedgerPluginConnectedEvent}.
   */
  @Override
  public void onConnect(LedgerPluginConnectedEvent event) {
    logger.info("LedgerPlugin Connected: {}", event);
  }

  /**
   * Called to handle an {@link LedgerPluginDisconnectedEvent}.
   *
   * @param event A {@link LedgerPluginDisconnectedEvent}.
   */
  @Override
  public void onDisconnect(LedgerPluginDisconnectedEvent event) {
    logger.info("LedgerPlugin Disconnected: {}", event);
  }

  /**
   * Called when an outgoing transfer prepared by this connector has been fulfilled.
   *
   * In Universal-mode, this connector is incentivized to pass this fulfillment back to the ledger that prepared the
   * corresponding source transfer so that this connector can capture any funds waiting for it.
   *
   * In Atomic-mode, this is less important, because the ledger underlying the source transfer will not fulfill until it
   * consults its notary. However, passing the fulfillment back to the source ledger might still be desirable in order
   * to prompt the source ledger to consult with its notary.
   *
   * @param event A {@link OutgoingTransferFulfilledEvent}.
   */
  @Override
  public void onTransferFulfilled(final OutgoingTransferFulfilledEvent event) {
    Objects.requireNonNull(event);

    final Transfer executedDestinationTransfer = event.getTransfer();
    final Fulfillment executionFulfillment = event.getFulfillment();
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Received notification about executed destination transfer with TransferId: {} on Ledger: {}",
          executedDestinationTransfer.getTransferId(),
          executedDestinationTransfer.getLedgerPrefix());
    }

    final TransferCorrelation transferCorrelation = this.getLedgerPluginManager()
        .getTransferCorrelationRepository()
        .findByDestinationTransferId(executedDestinationTransfer.getTransferId())
        .orElseThrow(() -> new RuntimeException(
            String.format(
                "Unable to fulfill source transfer for supplied destination transfer due to missing "
                    + "TransferCorrelation subprotocol info! DestinationTransfer: %s;",
                executedDestinationTransfer))
        );

    final Transfer sourceTransfer = transferCorrelation.getSourceTransfer();

    // If the destination transfer was executed, the connector should try to execute the source transfer to get paid.
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Requesting fulfillment of source transfer: {} (fulfillment: {}", sourceTransfer,
          executionFulfillment
      );
    }

    final LedgerPlugin sourceLedgerPlugin = this.getLedgerPluginManager()
        .getLedgerPluginSafe(sourceTransfer.getTransferId(), sourceTransfer.getLedgerPrefix());

    // TODO: Account for retries?

    try {
      sourceLedgerPlugin
          .fulfillCondition(sourceTransfer.getTransferId(), executionFulfillment);
    } catch (Exception e) {
      logger.error(
          "Attempted to execute source transfer but it was unsuccessful; we have not been fully re-paid! "
              + "SourceTransfer: {}, fulfillment: {}", sourceTransfer, executionFulfillment
      );
      throw e;
    }
  }

  /**
   * Called to handle an {@link IncomingMessgeRequestEvent}.
   *
   * @param event A {@link IncomingMessgeRequestEvent}.
   */
  @Override
  public void onMessageRequest(IncomingMessgeRequestEvent event) {
    // TODO: Implement this!
    throw new RuntimeException("Not yet implemented!");
  }

  /**
   * Called to handle an {@link OutgoingMessgeRequestEvent}.
   *
   * @param event A {@link OutgoingMessgeRequestEvent}.
   */
  @Override
  public void onMessageRequest(OutgoingMessgeRequestEvent event) {
    // TODO: Implement this!
    throw new RuntimeException("Not yet implemented!");
  }

}
