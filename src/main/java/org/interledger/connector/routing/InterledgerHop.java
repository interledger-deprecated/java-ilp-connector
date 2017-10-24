package org.interledger.connector.routing;

import org.interledger.InterledgerAddress;

import org.immutables.value.Value;

import java.math.BigInteger;

/**
 * Represents a "next hop" for purposes of Interledger Routing.
 */
@Value.Immutable
public interface InterledgerHop {

  /**
   * The ledger prefix of the destination ledger to make the "next" local-ledger transfer in.
   * 
   * @return {InterledgerAddress}
   */
  InterledgerAddress getDestinationLedgerPrefix();

  /**
   * The ILP address of the account to credit funds to as part of the "next" local-ledger transfer (the source-account
   * will always be the account of the connector at the destination ledger).
   * 
   * @return {@link InterledgerAddress}
   */
  InterledgerAddress getDestinationLedgerCreditAccount();

  /**
   * The amount of the next-hop transfer.
   *
   * headCurve.amountAt(sourceAmount).toString(),
   * 
   * @return BigInteger
   */
  BigInteger getDestinationAmount();

  /**
   * The actual final amount of this transfer, once slippage is considered.
   *
   * quote.liquidityCurve.amountAt(sourceAmount).toString()
   * 
   * @return BigInteger
   */
  BigInteger getFinalAmount();

  /**
   * Determines if this hop is servicable by a ledger that is locally-connected to the Connector wanting to know about a
   * next-hop. If a hop is "final", it means that this connector is delivering this payment (as opposed to forwarding
   * it). *
   *
   * @return {@code true} if this hop is the final hop; {@code false} if this hop is an intermediate hop.
   *
   * @see "https://github.com/interledger/rfcs/issues/77"
   * 
   * @return boolean
   */
  boolean isFinal();

}
