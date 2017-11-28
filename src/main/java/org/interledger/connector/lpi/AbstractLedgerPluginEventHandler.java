package org.interledger.connector.lpi;

import org.interledger.InterledgerAddress;
import org.interledger.connector.ConnectorUtils;
import org.interledger.connector.config.ConnectorConfig;
import org.interledger.connector.config.ConnectorConfigurationService;
import org.interledger.connector.fx.FxEngine;
import org.interledger.connector.repository.ImmutableTransferCorrelation;
import org.interledger.connector.repository.TransferCorrelation;
import org.interledger.connector.routing.PaymentRouter;
import org.interledger.connector.routing.Route;
import org.interledger.connector.services.LedgerPluginManager;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.ilp.InterledgerProtocolError;
import org.interledger.ilp.InterledgerProtocolError.Builder;
import org.interledger.ilp.InterledgerProtocolError.ErrorCode;
import org.interledger.plugin.lpi.ImmutableTransfer;
import org.interledger.plugin.lpi.LedgerInfo;
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
import org.interledger.plugin.lpi.exceptions.LedgerPluginNotConnectedException;
import org.interledger.plugin.lpi.handlers.LedgerPluginEventHandler;

import com.google.common.annotations.VisibleForTesting;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.MonetaryConversions;

/**
 * An abstract implementation of {@link LedgerPluginEventHandler} that handles events from Ledger plugins running in a
 * Connector.
 */
public abstract class AbstractLedgerPluginEventHandler<T extends ConnectorConfig> implements LedgerPluginEventHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // Provided by a Connector or higher level system to seed a deterministic identifier-generating function.
  private final String deterministicIdSecret;

  private final ConnectorConfigurationService<T> connectorConfigurationService;
  private final LedgerPluginManager ledgerPluginManager;
  private final PaymentRouter paymentRouter;
  private final FxEngine fxEngine;

  public AbstractLedgerPluginEventHandler(
      final String deterministicIdSecret,
      final ConnectorConfigurationService<T> connectorConfigurationService,
      final LedgerPluginManager ledgerPluginManager,
      final PaymentRouter paymentRouter,
      final FxEngine fxEngine
  ) {
    this.connectorConfigurationService = connectorConfigurationService;
    this.ledgerPluginManager = Objects.requireNonNull(ledgerPluginManager);
    this.deterministicIdSecret = Objects.requireNonNull(deterministicIdSecret);
    this.paymentRouter = Objects.requireNonNull(paymentRouter);
    this.fxEngine = Objects.requireNonNull(fxEngine);
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
      final Route nextHop = this.getPaymentRouter().findBestNexHop(
          sourceTransfer.getInterlederPaymentPacket().getDestinationAccount(),
          sourceTransfer.getLedgerPrefix()
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
      final Transfer sourceTransfer, final Route nextHopRoute
  ) {
    Objects.requireNonNull(sourceTransfer);
    Objects.requireNonNull(nextHopRoute);

    // No need to verify connectivity to the destination ledger here, because this will either succeed or
    // fail in the prepareDestinationTransfer call...

    // Check if this connector can authorize the final sourceTransfer.
    // If this connector/ilp-node is locally peered with a ledger, then we don't need to forward the payment anywhere.
    // However, we still need to perform FX since the source ledger will likely be different from the destination
    // ledger.
    final boolean nextHopIsLocallyPeered = this.ledgerPluginManager
        .isLocallyPeered(nextHopRoute.getNextHopLedgerPrefix());

    // The address of this connector's account on the underlying source ledger...
    final InterledgerAddress myAddress = getLedgerPluginManager().getLedgerPluginSafe(
        sourceTransfer.getTransferId(), sourceTransfer.getLedgerPrefix())
        .getConnectorAccount();

    final InterledgerPayment ilpPaymentPacket = sourceTransfer.getInterlederPaymentPacket();

    final InterledgerAddress nextHopCreditAccount;
    final BigInteger nextHopAmount;
    if (nextHopIsLocallyPeered) {
      // Since this connector is locally connected to the next-hop ledger, we technically could just sent the amount in
      // the ILP packet. However, this section performs one final check to ensure that this connector doesn't lose money
      // by blindly sending that amount without verifying that the amount received by the incoming transfer (on the
      // source ledger) is sufficient to complete this payment. This method verifies this by computing the current FX
      // rate between the source+destination local ledgers, and then checking to make sure that the incoming amount is
      // enough to satisfy the outgoing payment that is about to be made.

      // This is the amount we expect to send to the next-hop ledger, derived from the incoming source amount. If this
      // amount is too low (i.e., lower than the amount indicated in the ILP packet), we abort this payment because we
      // didn't receive enough incoming funds, and the receiver will likely reject our follow-on payment because it
      // won't be high enough (we don't worry about the amount being too high -- if the incoming amount is higher than
      // what we need to send to the receiver, then we simply pocket the difference).
      // For more discussion about this, see here: https://github.com/interledger/rfcs/issues/316
      final BigInteger derivedNextHopAmount = this.computeNextHopLocalTransferAmount(
          sourceTransfer, nextHopRoute.getNextHopLedgerPrefix()
      );

      // Verify derivedNextHopAmount ≤ ilpPacketAmountWithSlippage
      // As long as the (added fxSpread) > (slippage lost), the connector won't lose money.

      // The minimum amount that will be acceptable to the ILP receiver (i.e., the amount in the ILP packet minus some
      // slippage).
      final BigInteger ilpPacketAmountWithSlippage = this
          .calculateIlpPacketDestinationAmountWithSlippage(ilpPaymentPacket.getDestinationAmount(),
              ilpPaymentPacket.getDestinationAccount().getPrefix());

      // If the amount in the ILP packet (including slippage) is greater than the derivedNextHopAmount, then it means
      // the incoming transfer didn't have enough money, and an error should be thrown. We know the incoming transfer
      // didn't have enough because the derivedNextHopAmount is based upon that incoming amount (converted to the
      // destination ledger units).

      if (this.incomingTransferAmountIsInsufficient(ilpPacketAmountWithSlippage, derivedNextHopAmount)) {
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

      // Since this is the final hop, we can merely payout to the destination account in the ILP packet (because this
      // connector is greedy, we don't pay-out the derivedNextHopAmount).
      nextHopCreditAccount = ilpPaymentPacket.getDestinationAccount();
      nextHopAmount = ilpPaymentPacket.getDestinationAmount();

    } else {
      // This is not the final "hop" for this payment, so the address/amount should come from the routing table.
      nextHopCreditAccount = nextHopRoute.getNextHopLedgerAccount();
      // Compute the next-hop amount from the FxEngine. We
      nextHopAmount = this.computeNextHopLocalTransferAmount(
          sourceTransfer, nextHopRoute.getNextHopLedgerPrefix()
      );
    }

    // The ID for the next sourceTransfer should be deterministically generated, so that the connector doesn't send
    // duplicate outgoing transfers if it receives duplicate notifications of incoming payments. The deterministic
    // generation should ideally be impossible for a third party to predict. Otherwise an attacker might be able to
    // squat on a predicted ID in order to interfere with a payment or make a connector look unreliable. In order to
    // assure this, the connector may use a secret that seeds the deterministic ID generation.
    final TransferId destinationTransferId = ConnectorUtils.generateDeterministicTransferId(
        deterministicIdSecret, sourceTransfer.getLedgerPrefix(), sourceTransfer.getTransferId()
    );

    // The "next-hop" sourceTransfer
    final Transfer destinationTransfer = ImmutableTransfer.builder()
        .transferId(destinationTransferId)
        .ledgerPrefix(nextHopRoute.getNextHopLedgerPrefix())
        // The "source" account for this transfer should be this connector's account on the destination leger.
        .sourceAccount(
            // If the source plugin is not connected at this point, then something went wrong, and an exception
            // should be thrown, but ultimately this operation should simply be retried (assuming this event is
            // queued).
            this.getLedgerPluginManager()
                .getLedgerPluginSafe(destinationTransferId,
                    nextHopRoute.getNextHopLedgerPrefix())
                .getConnectorAccount()
        )
        .amount(nextHopAmount)
        // This is the destination account on the "next-hop" ledger, as found in the routing table.
        .destinationAccount(nextHopCreditAccount)
        .interlederPaymentPacket(sourceTransfer.getInterlederPaymentPacket())
        .executionCondition(sourceTransfer.getExecutionCondition())
        .cancellationCondition(sourceTransfer.getCancellationCondition())
        .expiresAt(this.computeDestinationTransferExpiry(sourceTransfer.getExpiresAt()))
        // TODO: Atomic-mode "cases"?
        .build();

    return destinationTransfer;
  }

  /**
   * <p>Computes the expiration date/time of an outgoing transfer, given the expiration date/time of a corresponding
   * incoming transfer.</p>
   *
   * <p>This value allows connectors to determine the window of time required between when an incoming and outgoing
   * transfer should expire. Connectors set this window such that they are confident that they will be able to timely
   * deliver a fulfillment back to the source ledger, even if the outgoing transfer is executed at the last possible
   * moment.</p>
   *
   * @param sourceExpiryInstant The {@link Instant} in time that a source-transfer expires.
   */
  @VisibleForTesting
  protected Instant computeDestinationTransferExpiry(final Instant sourceExpiryInstant) {
    Objects.requireNonNull(sourceExpiryInstant);

    return sourceExpiryInstant.minus(this.connectorConfigurationService.getConnectorConfig().getTransferExpiryWindow());
  }

  /**
   * Given a source transfer, compute the amount (in local units of the destination ledger) that should be transferred
   * to the next-hop local ledger.
   *
   * @param sourceTransfer          A {@link Transfer} with information from the incoming source transfer delivered to
   *                                this connector as part of a broader Interledger payment.
   * @param destinationLedgerPrefix An {@link InterledgerAddress} prefix for the destination ledger that the next-hop
   *                                tranfser will be delivered on.
   */
  @VisibleForTesting
  protected BigInteger computeNextHopLocalTransferAmount(
      final Transfer sourceTransfer,
      final InterledgerAddress destinationLedgerPrefix
  ) {
    Objects.requireNonNull(sourceTransfer);
    InterledgerAddress.requireAddressPrefix(destinationLedgerPrefix);

    final CurrencyUnit baseCurrencyUnit = this.ledgerPluginManager.getLedgerPlugin(sourceTransfer.getLedgerPrefix())
        .map(LedgerPlugin::getLedgerInfo)
        .map(LedgerInfo::getCurrencyUnit)
        .orElseThrow(() -> new LedgerPluginNotConnectedException(sourceTransfer.getLedgerPrefix()));

    final CurrencyUnit terminatingCurrencyUnit = this.ledgerPluginManager.getLedgerPlugin(destinationLedgerPrefix)
        .map(LedgerPlugin::getLedgerInfo)
        .map(LedgerInfo::getCurrencyUnit)
        .orElseThrow(() -> new LedgerPluginNotConnectedException(destinationLedgerPrefix));

    // This method does not catch the CurrencyConversionException because there's nothing to be done if that occurs. It
    // means that FX is not configured between the source/destination ledgers.
    return Optional.ofNullable(this.fxEngine.getExchangeRate(baseCurrencyUnit, terminatingCurrencyUnit))
        .map(fxRate -> {
          final MonetaryAmount sourceTransferMonetaryAmount = Money.of(sourceTransfer.getAmount(), baseCurrencyUnit);
          final CurrencyConversion conversion = MonetaryConversions.getConversion(terminatingCurrencyUnit);
          final MonetaryAmount terminatingMonetaryAmount = sourceTransferMonetaryAmount.with(conversion);
          return terminatingMonetaryAmount;
        })
        // Convert from a MonetaryAmount back to an Integer...
        .map(MonetaryAmount::getNumber)
        .map(numberValue -> numberValue.numberValueExact(BigInteger.class))
        // This is allowed because if fxEngine.getExchangeRate can't find a rate, it will throw an exception!
        .get();
  }

  /**
   * Given an ILP payment package final destination amount, reduce the value by a configured amount of allowable
   * slippage.
   *
   * Slippage occurs when the actual FX-rates between a quoted amount and an actual amount diverge due to various
   * factors outside of the control of the FX engine. For example, during quoting, the FX-rate may be 2:1, meaning 2
   * units of a source currency can be exchanged for 1 unit of a terminating currency. Thus, a sender sending 2 units
   * should expect 1 unit to arrive at the final destination. However, if by the time the payment is actually made, the
   * rates have "slipped" to 3:1, it is now more expensive for the sender to get 1 unit to the receiver. Thus, sending 2
   * units would only allow 2/3 of the terminating unit to arrive at the reciever, which means the amount of money the
   * receiver would receive has also "slipped." Because certain ledgers cannot guarantee quoted pricing, this connector
   * tolerates a certain amount of slippage as configured in {@link ConnectorConfig#getDefaultSlippagePercent()}.
   *
   * @param ilpPaymentDestinationAmount A {@link BigInteger} representing the amount, in units of the final-destination
   *                                    ledger, of the ILP payment.
   */
  @VisibleForTesting
  protected BigInteger calculateIlpPacketDestinationAmountWithSlippage(
      final BigInteger ilpPaymentDestinationAmount,
      final InterledgerAddress destinationLedgerPrefix
  ) {
    Objects.requireNonNull(ilpPaymentDestinationAmount);
    InterledgerAddress.requireAddressPrefix(destinationLedgerPrefix);

    // The amount can "slip" down by up-to {slippagePercent}
    final BigDecimal slippagePercent = this.connectorConfigurationService.getConnectorConfig()
        .getDefaultSlippagePercent();

    // Formula: amount * (1 - slippagePercent) => 500 * (1 - 1%) => 500 * (0.99) => 495
    final BigDecimal percentageInDecimal = BigDecimal.ONE.subtract(slippagePercent);

    return new BigDecimal(ilpPaymentDestinationAmount)
        // Uses infinite precision, which is simply signficant digits (not scale nor total number of digits).
        .multiply(percentageInDecimal).setScale(0, RoundingMode.HALF_UP)
        // Should never throw an exception because scale was set to 0 above.
        .toBigIntegerExact();
  }

  /**
   * Determines if the incoming transfer amount is insufficient.
   *
   * When attempting to determine the next-hop transfer amount to send as part of an Interledger payment, this connector
   * needs to ensure that it sends enough funds to get the receiver to fulfill the payment, but not so many funds that
   * this connector would lose money.
   *
   * To avoid losing money, this connector must not send more money to the next-hop than it received on the incoming
   * hop. This is accomplished by adding an FX-margin to the next-hop amount in order to ensure the amount sent out is
   * less-than the amount received in the incoming transfer.
   *
   * However, to ensure that the receiver fulfills the payment, the amount of the next-hop destination transfer must be
   * greater-than or equal to the amount (adjusted for slippage, if any) in the ILP packet (this is the amount that the
   * receiver is expecting to receive).
   *
   * Both of these concerns can be addressed as long as the amount in the packet is greater-than or equal-to the
   * calculated next-hop amount (padding with FX spread).
   *
   * For example, consider the following scenarios (each with a 1:1 FX rate and no slippage, for simplicity).
   *
   * 1.) An ILP Payment of $1 is sent with an incoming transfer of $1 into an account owned by this connector. The
   * FxEngine calculates that the outgoing payment should be $0.98 (a 2% FxSpread based upon the incoming transfer
   * amount. Since the ILP Packet amount ($1) is greater-than the amount about to be sent to the next-hop ($0.98), the
   * transfer will fail with an R01_INSUFFICIENT_SOURCE_AMOUNT error will be returned.
   *
   * 2.) An ILP Payment of $1 is sent with an incoming transfer of $2 into an account owned by this connector. The
   * FxEngine calculates that the outgoing payment should be $2 (a 2% FxSpread based upon the incoming transfer amount).
   * Since the ILP Packet amount ($1) is less-than the amount about to be sent to the next-hop ($2), the transfer will
   * succeed in the amount of $1 (which is the amount in the ILP packet) because this connector is greedy.
   *
   * 3.) An ILP Payment of $1 is sent with an incoming transfer of $0.95 into an account owned by this connector. The
   * FxEngine calculates that the outgoing payment should be $0.931 (a 2% FxSpread based upon the incoming transfer
   * amount). Since the ILP Packet amount ($1) is greater-than the amount about to be sent to the next-hop ($0.931), the
   * transfer will fail with an R01_INSUFFICIENT_SOURCE_AMOUNT error will be returned.
   *
   * 4.) An ILP Payment of $1 is sent with an incoming transfer of $1.02 into an account owned by this connector. The
   * FxEngine calculates that the outgoing payment should be $1 (a 2% FxSpread based upon the incoming transfer amount).
   * Since the ILP Packet amount ($1) is equal-to the amount about to be sent to the next-hop, the transfer will succeed
   * in the amount of $1 (which is the amount in the ILP packet).
   *
   * @param ilpPacketAmountWithSlippage A {@link BigInteger} representing the amount, in units of the final-destination
   *                                    ledger, of the ILP payment, minus allowed slippage (if any). In other words,
   *                                    this is the amount in the ILP packet, reduced by any slippage percentage
   *                                    configured in this connector.
   * @param calculatedNextHopAmount     A {@link BigInteger} representing the amount, in units of the next-hop ledger
   *                                    (which may or may not be the final destination ledger) that this connector
   *                                    _should_ pay to the next-hop, based solely on an FX+spread calculation using the
   *                                    amount in the incoming transfer.
   *
   * @see "https://github.com/interledger/rfcs/issues/316"
   */
  @VisibleForTesting
  protected boolean incomingTransferAmountIsInsufficient(
      final BigInteger ilpPacketAmountWithSlippage, final BigInteger calculatedNextHopAmount
  ) {
    return ilpPacketAmountWithSlippage.compareTo(calculatedNextHopAmount) > 0;
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

  public PaymentRouter<? extends Route> getPaymentRouter() {
    return paymentRouter;
  }
}
