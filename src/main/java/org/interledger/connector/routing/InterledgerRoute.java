package org.interledger.connector.routing;

import org.interledger.InterledgerAddress;
import org.interledger.connector.RouteId;

import org.immutables.value.Value;
import org.immutables.value.Value.Default;

/**
 * A "route" for purposes of Interledger payments. This design is very simple for current functionality, and currently
 * models a table that knows about all and looks like this:
 *
 * <pre>
 * ┌──────────────────────────┐│┌────────────────────────┐│┌────────┐
 * │Destination Ledger Prefix │││ Next Hop Ledger Prefix │││Num Hops│
 * └──────────────────────────┘│└────────────────────────┘│└────────┘
 * ────────────────────────────┼──────────────────────────┼──────────
 * ┌──────────────────────────┐│┌────────────────────────┐│┌────────┐
 * │        g.us.bank.        │││        g.hub1.         │││   4    │
 * ├──────────────────────────┤│├────────────────────────┤│├────────┤
 * │        g.eu.bank.        │││        g.hub2.         │││   2    │
 * └──────────────────────────┘│└────────────────────────┘│└────────┘
 * </pre>
 *
 * Eventually, this class needs to be refined so that it can properly model an appropriate route, likely taking into
 * account things like LiquidityCurve and Fees (i.e., a cost function).
 *
 * @see "https://github.com/interledgerjs/five-bells-shared/blob/v22.0.1/schemas/Routes.json"
 * @deprecated This interface will likely be replaced with a variant from java-ilp-core.
 */
@Deprecated
@Value.Immutable
public interface InterledgerRoute {

  RouteId getRouteId();
  // TODO

//    addedDuringEpoch = 0
//    additionalInfo = undefined
//        curve = LiquidityCurve
//    destinationAccount = "usd-ledger.mark"
//    destinationLedger = "usd-ledger."
//    expiresAt = undefined
//        isLocal = true
//    minMessageWindow = 1
//    nextLedger = "usd-ledger."
//    paths = Array[1]
//    sourceAccount = "eur-ledger.mark"
//    sourceLedger = "eur-ledger."
//    targetPrefix = "usd-ledger."


  /**
   * The ledger-prefix of the destination ledger for this route.
   * 
   * @return {@link InterledgerAddress}
   */
  InterledgerAddress getDestinationLedgerPrefix();

  /**
   * The ledger-prefix of the next-hop ledger that a payment should be forwarded to in order to complete an Interledger
   * payment.
   * 
   * @return {@link InterledgerAddress}
   */
  InterledgerAddress getNextHopLedgerPrefix();

  /**
   * The number of hops that this route will require in order to reach the destination.
   * 
   * @return Integer
   **/
  Integer getNumHops();

  /**
   * The target of this route. Defines the address or prefix that a particular message/packet should be routed
   * towards.
   */
  //InterledgerAddress getTargetInterledgerAddress();

  /**
   * The address-prefix of the ledger that will satisfy this route (in other words, a "ledger prefix").
   */
  //InterledgerAddress getLedgerPrefix();

  /**
   * The Interledger address of this connector's account on the above ledger.
   */
  //InterledgerAddress getConnectorAddress();

  /**
   * Determines if this route is "local", meaning the route's target is connected to this connector (i.e., the connector
   * holding the routing table).
   * 
   * @return boolean
   */
  @Default
  default boolean isLocal() {
    return false;
  }
}
