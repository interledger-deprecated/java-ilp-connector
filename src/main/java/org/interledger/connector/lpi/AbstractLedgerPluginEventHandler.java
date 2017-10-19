package org.interledger.connector.lpi;

import org.interledger.InterledgerAddress;
import org.interledger.connector.ConnectorUtils;
import org.interledger.connector.repository.ImmutableTransferCorrelation;
import org.interledger.connector.repository.TransferCorrelation;
import org.interledger.connector.routing.InterledgerHop;
import org.interledger.connector.routing.PaymentRouter;
import org.interledger.connector.services.LedgerPluginManager;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.ilp.InterledgerProtocolError;
import org.interledger.ilp.InterledgerProtocolError.Builder;
import org.interledger.ilp.InterledgerProtocolError.ErrorCode;
import org.interledger.plugin.lpi.ImmutableTransfer;
import org.interledger.plugin.lpi.LedgerPlugin;
import org.interledger.plugin.lpi.Transfer;
import org.interledger.plugin.lpi.TransferId;
import org.interledger.plugin.lpi.events.IncomingTransferCancelledEvent;
import org.interledger.plugin.lpi.events.IncomingTransferFulfilledEvent;
import org.interledger.plugin.lpi.events.IncomingTransferPreparedEvent;
import org.interledger.plugin.lpi.events.IncomingTransferRejectedEvent;
import org.interledger.plugin.lpi.events.LedgerPluginErrorEvent;
import org.interledger.plugin.lpi.events.OutgoingTransferCancelledEvent;
import org.interledger.plugin.lpi.events.OutgoingTransferPreparedEvent;
import org.interledger.plugin.lpi.events.OutgoingTransferRejectedEvent;
import org.interledger.plugin.lpi.exceptions.AccountNotFoundException;
import org.interledger.plugin.lpi.exceptions.DuplicateTransferIdentifier;
import org.interledger.plugin.lpi.exceptions.InsufficientBalanceException;
import org.interledger.plugin.lpi.exceptions.InvalidTransferException;
import org.interledger.plugin.lpi.exceptions.LedgerPluginException;
import org.interledger.plugin.lpi.handlers.LedgerPluginEventHandler;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * An abstract implementation of {@link LedgerPluginEventHandler} that handles events from Ledger plugins running in a
 * Connector.
 */
public abstract class AbstractLedgerPluginEventHandler implements LedgerPluginEventHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // Provided by a Connector or higher level system to seed a deterministic identifier generating
  // function.
  private final String deterministicIdSecret;

  private final LedgerPluginManager ledgerPluginManager;
  private final PaymentRouter paymentRouter;

  public AbstractLedgerPluginEventHandler(
    final String deterministicIdSecret, final LedgerPluginManager ledgerPluginManager,
    final PaymentRouter paymentRouter) {
    this.ledgerPluginManager = Objects.requireNonNull(ledgerPluginManager);
    this.deterministicIdSecret = Objects.requireNonNull(deterministicIdSecret);
    this.paymentRouter = Objects.requireNonNull(paymentRouter);
  }

  @Override
  public void onError(LedgerPluginErrorEvent event) {
    logger.error(
      "LedgerPlugin will disconnect and be removed from the LedgrePluginManager after encountering an unrecoverable Error: {}",
      event.getLedgerPrefix(), event.getError());

    // Remove ourselves from the active plugins, because something went horribly wrong...
    this.getLedgerPluginManager().removeLedgerPlugin(event.getLedgerPrefix());
  }

  @Override
  public void onTransferPrepared(IncomingTransferPreparedEvent event) {
    logger.info("onTransferPrepared: {}", event);

    // This method works by attempting to process the incoming transfer while if at any point a non-retryable error
    // is encountered, it is thrown as an InvalidTransferException, which is caught and then used to reject the
    // payment on the source ledger. Otherwise, non-InvalidTransferExceptions are simply thrown and logged by the
    // system, with implementations potentially choosing to implement queued notification handling that might
    // allow for retries after something like a bug or other temporary condition is fixed.

    final Transfer sourceTransfer = event.getTransfer();
    try {
      //this.validateIncomingPreparedTransfer(sourceTransfer);

      // The address of the ultimate receiver of this ILP Payment....
      final InterledgerPayment ilpPaymentPacket = sourceTransfer.getInterlederPaymentPacket();

      // The address of the connector account on the underlying source ledger...
      final InterledgerAddress myAddress = getLedgerPluginManager().getLedgerPluginSafe(
        sourceTransfer.getTransferId(), sourceTransfer.getLedgerPrefix())
        .getConnectorAccount();

      // Don't do anything with incoming ILP payments where this connector is the final receiver, because there is
      // no "next-hop" transfer to be made.
      if (ilpPaymentPacket.getDestinationAccount().startsWith(myAddress)) {
        logger.warn(
          "Ignoring Transfer to destination which starts with this plugin's address: "
            + "thisPlugin: \"{}\" ilpPayment Destination: \"{}\"",
          myAddress, ilpPaymentPacket.getDestinationAccount());
        return;
      }

      // Determine the nextHop for this payment....
      final InterledgerHop nextHop = this.getPaymentRouter().determineNexHop(
        sourceTransfer.getLedgerPrefix(),
        sourceTransfer.getInterlederPaymentPacket(),
        sourceTransfer.getAmount()
      )
        // If no hop can be determined, we immediately reject the source transfer.
        .orElseThrow(() -> new InvalidTransferException(
          String.format("No route found from \"%s\" to \"%s\"",
            myAddress,
            ilpPaymentPacket.getDestinationAccount()
          ),
          sourceTransfer.getSourceAccount(),
          sourceTransfer.getTransferId(),
          InterledgerProtocolError.builder()
            .errorCode(ErrorCode.F02_UNREACHABLE)
            .triggeredAt(Instant.now())
            .triggeredByAddress(myAddress)
            .build()
        ));

      final Transfer destinationTransfer = this.buildNextHopTransfer(sourceTransfer, nextHop);

      // Specifies which source_transfer to utilize when handling future reject/fulfill events on the
      // source and destination ledgers. This operation should be done before preparing the transfer
      // on the destination ledger. If that prepare fails, it will likely be retried, in which case
      // this call will merely overwrite itself, which is benign.
      final TransferCorrelation transferCorrelation = ImmutableTransferCorrelation.builder()
        .sourceTransfer(sourceTransfer)
        .destinationTransfer(destinationTransfer)
        .build();
      this.getLedgerPluginManager().getTransferCorrelationRepository()
        .save(transferCorrelation);

      // Prepare the transfer on the destination ledger...
      this.prepareDestinationTransfer(sourceTransfer, destinationTransfer);

    } catch (InvalidTransferException e) {
      // The transfer was invalid for whatever reason, so we should immediately reject it.
      logger.error("Rejecting Incoming Transfer: {}", e.getMessage(), e);
      this.getLedgerPluginManager()
        .getLedgerPluginSafe(sourceTransfer.getTransferId(),
          sourceTransfer.getLedgerPrefix())
        .rejectIncomingTransfer(sourceTransfer.getTransferId(), e.getRejectionReason());
      return;
    }
  }

  /**
   * Given a source transfer and information about the "next hop" in an Interledger payment chain, construct a new
   * {@link Transfer} that can be used to complete this Interledger payment.
   */
  protected Transfer buildNextHopTransfer(
    final Transfer sourceTransfer, final InterledgerHop nextHop
  ) {
    Objects.requireNonNull(sourceTransfer);
    Objects.requireNonNull(nextHop);

    // No need to verify connectivity to the destination ledger here, because this will either succeed or
    // fail in the prepareDestinationTransfer call...

    // The address of the connector account on the underlying source ledger...
    final InterledgerAddress myAddress = getLedgerPluginManager().getLedgerPluginSafe(
      sourceTransfer.getTransferId(), sourceTransfer.getLedgerPrefix())
      .getConnectorAccount();

    final InterledgerPayment ilpPaymentPacket = sourceTransfer.getInterlederPaymentPacket();

    // Check if this connector can authorize the final sourceTransfer.
    final InterledgerAddress nextHopCreditAccount;
    final BigInteger nextHopAmount;
    if (nextHop.isFinal()) {
      // TODO: Account for slippage?
      // Verify expectedFinalAmount ≤ actualFinalAmount
      // As long as the fxSpread > slippage, the connector won't lose money.
      final BigInteger slippage = BigInteger.ZERO;
      final BigInteger expectedFinalAmount = ilpPaymentPacket.getDestinationAmount()
        .multiply(BigInteger.ONE.subtract(slippage));
      // If the expectedFinalAmount is greater than the actual final amount, then this sourceTransfer doesn't have
      // enough funds in it.
      if (expectedFinalAmount.compareTo(nextHop.getFinalAmount()) > 0) {
        new InvalidTransferException(
          "Payment rate does not match the rate currently offered",
          sourceTransfer.getLedgerPrefix(),
          sourceTransfer.getTransferId(),
          InterledgerProtocolError.builder()
            .errorCode(ErrorCode.R01_INSUFFICIENT_SOURCE_AMOUNT)
            .triggeredAt(Instant.now())
            .triggeredByAddress(myAddress)
            .build()
        );
      }

      // Since this is the final hop, we can merely payout to the destination account in the ILP packet.
      nextHopCreditAccount = ilpPaymentPacket.getDestinationAccount();
      nextHopAmount = ilpPaymentPacket.getDestinationAmount();
    } else {
      // This is not the final "hop" for this payment, so the address/amount should come from the routing table.
      nextHopCreditAccount = nextHop.getDestinationLedgerCreditAccount();
      nextHopAmount = nextHop.getDestinationAmount();
    }

    // The ID for the next sourceTransfer should be deterministically generated, so that the connector doesn't send
    // duplicate outgoing transfers if it receives duplicate notifications. The deterministic generation should
    // ideally be impossible for a third party to predict. Otherwise an attacker might be able to squat on a
    // predicted ID in order to interfere with a payment or make a connector look unreliable. In order to assure
    // this, the connector may use a secret that seeds the deterministic ID generation.
    final TransferId destinationTransferId = ConnectorUtils.generateTransferId(
      deterministicIdSecret, sourceTransfer.getLedgerPrefix(), sourceTransfer.getTransferId()
    );

    // The "next-hop" sourceTransfer
    final Transfer destinationTransfer = ImmutableTransfer.builder()
      .transferId(destinationTransferId)
      .ledgerPrefix(nextHop.getDestinationLedgerPrefix())
      // The "source" account for this transfer should be this connector's account on the destination leger.
      .sourceAccount(
        // If the source plugin is not connected at this point, then something went wrong, and an exception
        // should be thrown, but ultimately this operation should simply be retried (assuming this event is
        // queued).
        this.getLedgerPluginManager()
          .getLedgerPluginSafe(destinationTransferId,
            nextHop.getDestinationLedgerPrefix())
          .getConnectorAccount()
      )
      .amount(nextHopAmount)
      .destinationAccount(nextHopCreditAccount)
      .interlederPaymentPacket(sourceTransfer.getInterlederPaymentPacket())
      .executionCondition(sourceTransfer.getExecutionCondition())
      .cancellationCondition(sourceTransfer.getCancellationCondition())
      .expiresAt(sourceTransfer.getExpiresAt())
      // TODO: Atomic-mode "cases"?
      .build();

    return destinationTransfer;
  }

  /**
   * Called when an incoming transfer has been fulfilled on the underlying ledger. For standard behavior, this is a
   * no-op because in general, this connector was the one that would have passed the fulfillment to that ledger plugin.
   *
   * @param event A {@link IncomingTransferFulfilledEvent}.
   */
  @Override
  public void onTransferFulfilled(IncomingTransferFulfilledEvent event) {
    // No-Op.
    if (logger.isDebugEnabled()) {
      logger
        .debug(
          "Incoming Transfer intended for this Connector successfully fulfilled by this Connector: {}",
          event);
    }
  }

  /**
   * Called when an incoming transfer has expired on the underlying ledger. This event may be emitted by the
   * ledger-plugin, but it might also be emitted by an Atomic-mode validator.
   *
   * For this implementation, this is currently a no-op. However, if this is occurring in a Universal-Mode Connector, it
   * may be desirable to track this, because it _may_ have occurred whilst an outgoing transfer was waiting to be
   * prepared, and/or might have been fulfilled. In that instance, this connector likely would lose money.
   *
   * In Atomic-Mode usage, this callback merely indicates that the source-transfer ledger cancelled the transaction.
   * However, this is likely still just a no-op because either no destination transfer has yet been prepared, in which
   * case nothing need happen. Or, the destination ledger will consult the same Atomic-Mode notary and likewise reject
   * the destination transfer, meaning no action is necessary here.
   *
   * One optimization that could be made is for the Connector to respond to this method by checking to see if any
   * incoming_transfer events have yet to be processed, and preemptively _not_ prepare on the destination ledger, but
   * this is likely an optimization that would slow-down the happy path due to checking in the prepare handler, so the
   * optimization may not be worth it.
   *
   * @param event A {@link IncomingTransferCancelledEvent}.
   */
  @Override
  public void onTransferCancelled(IncomingTransferCancelledEvent event) {
    // No-Op.
    if (logger.isDebugEnabled()) {
      logger
        .debug("Incoming Transfer intended for this Connector expired: {}", event);
    }
  }

  /**
   * Called when an incoming transfer that was rejected by this connector has completed its rejection.
   *
   * @param event A {@link IncomingTransferRejectedEvent}.
   */
  @Override
  public void onTransferRejected(IncomingTransferRejectedEvent event) {
    // No-Op.
    if (logger.isDebugEnabled()) {
      logger
        .debug(
          "Incoming Transfer intended for this Connector successfully rejected by this Connector: {}",
          event);
    }
  }

  /**
   * Called when an outgoing transfer has been prepared on the underlying, destination ledger. For standard behavior,
   * this is a no-op because in general, this connector was the one that would have prepared the transfer in the first
   * place, so other than logging the notification, there's nothing more to be done here.
   *
   * @param event A {@link OutgoingTransferPreparedEvent}.
   */
  @Override
  public void onTransferPrepared(final OutgoingTransferPreparedEvent event) {
    Objects.requireNonNull(event);
    // No-Op.
    if (logger.isDebugEnabled()) {
      logger
        .debug("Outgoing Transfer from this Connector successfully prepared: {}", event);
    }
  }

  /**
   * This is event is emitted if an outgoing transfer prepared by this connector is cancelled by the destination ledger
   * (i.e., it timed out).
   *
   * @param event A {@link OutgoingTransferCancelledEvent}.
   */
  @Override
  public void onTransferCancelled(OutgoingTransferCancelledEvent event) {
    this.rejectSourceTransferForDestination(event.getTransfer(), event.getCancellationReason());
  }

  /**
   * This is event is emitted if an outgoing transfer prepared by this connector is rejected by the destination ledger.
   *
   * @param event A {@link OutgoingTransferCancelledEvent}.
   */
  @Override
  public void onTransferRejected(OutgoingTransferRejectedEvent event) {
    Objects.requireNonNull(event);
    this.rejectSourceTransferForDestination(event.getTransfer(), event.getRejectionReason());
  }

  ////////////////////
  // Helper methods...
  ////////////////////

//  @VisibleForTesting
//  protected void validateIncomingPreparedTransfer(final Transfer transfer) {
//    Objects.requireNonNull(transfer);
//
//    // The expected ledger prefix for the incoming transfer.
//    final InterledgerAddress expectedLedgerPrefix = this.ledgerPluginConfig.getLedgerPrefix();
//
//    // Ensure that the transfer's ledger-prefix matches the ledger prefix of this plugin.
//    if (!expectedLedgerPrefix.equals(transfer.getLedgerPrefix())) {
//      throw new InvalidTransferException(
//          String.format("Unsupported Transfer Ledger Prefix \"%s\" for LedgerPlugin prefix \"%s\"!",
//              transfer.getLedgerPrefix(), expectedLedgerPrefix),
//          expectedLedgerPrefix,
//          transfer.getTransferId(),
//          InterledgerProtocolError.builder()
//              .errorCode(ErrorCode.F00_BAD_REQUEST)
//              // TODO: https://github.com/interledger/java-ilp-core/issues/82
//              .triggeredAt(Instant.now())
//              .triggeredByAddress(expectedLedgerPrefix)
//              .build()
//      );
//    }
//
//    // Ensure that the destination account is this plugin's connector account.
//    if (!transfer.getDestinationAccount().startsWith(expectedLedgerPrefix)) {
//      throw new InvalidTransferException(
//          String.format("Invalid _destination_ account: \"%s\" for LedgerPlugin: \"%s\"!",
//              transfer.getSourceAccount(), expectedLedgerPrefix),
//          expectedLedgerPrefix,
//          transfer.getTransferId(),
//          InterledgerProtocolError.builder()
//              .errorCode(ErrorCode.F00_BAD_REQUEST)
//              // TODO: https://github.com/interledger/java-ilp-core/issues/82
//              .triggeredAt(Instant.now())
//              .triggeredByAddress(expectedLedgerPrefix)
//              .build()
//      );
//    }
//
//    // Ensure that the source account is correct for the ledger prefix.
//    if (!transfer.getSourceAccount().startsWith(expectedLedgerPrefix)) {
//      throw new InvalidTransferException(
//          String.format("Invalid _source_ account: \"%s\" for LedgerPlugin: \"%s\"!",
//              transfer.getSourceAccount(), expectedLedgerPrefix),
//          expectedLedgerPrefix,
//          transfer.getTransferId(), InterledgerProtocolError.builder()
//          .errorCode(ErrorCode.F00_BAD_REQUEST)
//          // TODO: https://github.com/interledger/java-ilp-core/issues/82
//          .triggeredAt(Instant.now())
//          .triggeredByAddress(expectedLedgerPrefix)
//          .build()
//      );
//    }
//  }

  /**
   * Given a prepared source transfer, and a ready to transmit outgoing transfer, prepare the destination transfer on
   * the appropriate ledger plugin.
   *
   * If the destination transfer cannot be prepared, for whatever reason, then reject the incoming source transfer with
   * an appropriate ILP error code.
   */
  @VisibleForTesting
  protected void prepareDestinationTransfer(final Transfer sourceTransfer,
    final Transfer destinationTransfer) {
    if (logger.isDebugEnabled()) {
      logger.debug("About to settle payment. Source: {}; Destination Transfer: {}",
        sourceTransfer,
        destinationTransfer);
    }

    // Before trying to settle, we should ensure that the connector is connected to both the source and destination
    // via the correct ledger plugins. If resolving these fails for any reason, then this is a runtime error, and
    // should not trigger any responses to the source ledger (in other words, this is like a precondition).

    final LedgerPlugin destinationLedgerPlugin = this.getLedgerPluginManager()
      .getLedgerPluginSafe(destinationTransfer.getTransferId(),
        sourceTransfer.getLedgerPrefix());

    try {
      destinationLedgerPlugin.sendTransfer(destinationTransfer);
    } catch (LedgerPluginException lpe) {
      // Map the LedgerPluginException to a proper RejectionMessage that can be sent back to the source ledger plugin.
      final InterledgerProtocolError rejectionReason = this
        .fromLedgerPluginException(destinationTransfer.getLedgerPrefix(), lpe);

      // If the source ledger plugin cannot be located, this is definitely a runtime exception, which can simply
      // be emitted and handled by the caller of this method. However, no exception is expected, so we reject the
      // source transfer on the located ledger plugin.
      this.getLedgerPluginManager()
        .getLedgerPluginSafe(sourceTransfer.getTransferId(),
          sourceTransfer.getLedgerPrefix())
        .rejectIncomingTransfer(sourceTransfer.getTransferId(), rejectionReason);
    }
  }

  /**
   * Map an instance of {@link LedgerPluginException} to a corresponding {@link InterledgerProtocolError} for sending
   * back to a source ledger.
   */
  @VisibleForTesting
  protected InterledgerProtocolError fromLedgerPluginException(
    final InterledgerAddress triggeringLedgerPrefix, final LedgerPluginException lpe
  ) {
    Objects.requireNonNull(triggeringLedgerPrefix);

    return Optional.of(lpe)
      .map(exception -> {
        final Builder builder = InterledgerProtocolError.builder()
          .triggeredAt(Instant.now())
          .triggeredByAddress(triggeringLedgerPrefix);
        if (exception instanceof DuplicateTransferIdentifier
          || exception instanceof InvalidTransferException) {
          builder.errorCode(ErrorCode.F00_BAD_REQUEST);
        } else if (exception instanceof InsufficientBalanceException) {
          builder.errorCode(ErrorCode.T04_INSUFFICIENT_LIQUIDITY);
        } else if (exception instanceof AccountNotFoundException) {
          builder.errorCode(ErrorCode.F02_UNREACHABLE);
        } else {
          builder.errorCode(ErrorCode.T01_LEDGER_UNREACHABLE);
        }
        return builder.build();
      }).get();
  }

  /**
   * This method rejects a source transfer where there is a rejected destination transfer.
   *
   * @param rejectedDestinationTransfer A destination {@link Transfer}
   * @param rejectionReason             A {@link InterledgerProtocolError} containing information from the destination
   *                                    ledger about why that destination transfer was rejected.
   */
  @VisibleForTesting
  protected void rejectSourceTransferForDestination(
    final Transfer rejectedDestinationTransfer, final InterledgerProtocolError rejectionReason
  ) {
    final TransferCorrelation transferCorrelation = this.getLedgerPluginManager()
      .getTransferCorrelationRepository()
      .findByDestinationTransferId(rejectedDestinationTransfer.getTransferId())
      .orElseThrow(() -> new RuntimeException(String.format(
        "Unable to reject source transfer for supplied destination transfer due to missing "
          + "TransferCorrelation info! "
          + "DestinationTransfer: %s; rejectionReason: %s",
        rejectedDestinationTransfer, rejectionReason))
      );

    final TransferId sourceTransferId = transferCorrelation.getSourceTransfer().getTransferId();
    final InterledgerAddress sourceLedgerPrefix = transferCorrelation.getSourceTransfer()
      .getLedgerPrefix();

    final LedgerPlugin sourceLedgerPlugin = this.getLedgerPluginManager()
      .getLedgerPluginSafe(sourceTransferId, sourceLedgerPrefix);

    final InterledgerProtocolError forwardedRejectionReason = InterledgerProtocolError
      .withForwardedAddress(rejectionReason, sourceLedgerPlugin.getConnectorAccount());
    sourceLedgerPlugin.rejectIncomingTransfer(sourceTransferId, forwardedRejectionReason);
  }

  public LedgerPluginManager getLedgerPluginManager() {
    return this.ledgerPluginManager;
  }

  public PaymentRouter getPaymentRouter() {
    return paymentRouter;
  }
}
