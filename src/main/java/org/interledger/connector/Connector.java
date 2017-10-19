package org.interledger.connector;


import org.interledger.connector.config.ConnectorConfigurationService;
import org.interledger.connector.services.LedgerPluginManager;

/**
 * This interface defines an Interledger Connector, which orchestrates any two ledgers to facilitate an Interledger
 * payment.
 *
 * <p>The Connector serves three primary purposes:</p>
 *
 * <pre>
 * 1. To hold accounts on various ledgers so that it can provide liquidity across ledgers.
 * 3. To provide route information and routing updates to other connectors.
 * 3. To provide quotes for a given ledger-to-ledger transfer.
 * </pre>
 *
 * <p>Interledger Payments moves asset representations (e.g., currency, stock, IOUs, gold, etc) from one party to
 * another by utilizing one or more ledger transfers, potentially across multiple ledgers.</p>
 *
 * <p> When a sender prepares a transfer on a Ledger to start a payment, the sender attaches an ILP Payment to the
 * ledger transfer, in the memo field if possible. If a ledger does not support attaching the entire ILP Payment to a
 * transfer as a memo, users of that ledger can transmit the ILP Payment using another authenticated messaging channel,
 * but MUST be able to correlate transfers and ILP Payments.</p>
 *
 * <p> When a connector sees an incoming prepared transfer with an ILP Payment, it reads the ILP Payment information to
 * confirm the details of the packet. For example, the connector reads the InterledgerAddress of the payment's receiver,
 * and if the connector has a route to the receiver's account, the connector prepares a transfer to continue the payment
 * chain by attaching the same ILP Payment to the new transfer.</p>
 *
 * <p>At the end of the payment chain, the final receiver (or, more likely, that ledger acting on behalf of the final
 * receiver) confirms that the amount in the ILP Payment Packet matches the amount actually delivered by the transfer.
 * Finally, the last-hop ledger decodes the data portion of the Payment and matches the condition to the payment. The
 * final Interledger node MUST confirm the integrity of the ILP Payment, for example with a hash-based message
 * authentication code (HMAC). If the receiver finds the transfer acceptable, the receiver releases the fulfillment for
 * the transfer, which can be used to execute all prepared transfers that were established prior to the receiver
 * accepting the payment.</p>
 */
public interface Connector<T extends ConnectorConfigurationService> {

  /**
   * Accessor for the {@link LedgerPluginManager} that is used to centralize all interactions with ledger plugins for a
   * given Connector.
   */
  LedgerPluginManager getLedgerPluginManager();

  T getConnectorConfigurationService();

  // TODO: Router, Quoter

}