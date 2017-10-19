package org.interledger.connector.quoting;

import org.interledger.connector.RouteId;
import org.interledger.connector.routing.InterledgerHop;

import java.math.BigInteger;
import java.time.Instant;

import javax.money.convert.ExchangeRate;

/**
 * Defines an Interledger payment quote that can be used to assemble a "next-hop" transfer on a
 * destination ledger, in response to an incoming transfer on a different ledger.
 */
public interface Quote {

  /**
   * The unique identifier of the liquidity path this quote is valid for.
   */
  RouteId getRouteId();

  /**
   * The next "hop" that this quote exists for.
   */
  InterledgerHop getNextHop();

  /**
   * The exchange-rate applied to this quote.
   */
  ExchangeRate getExchangeRate();

  /**
   * The amount to be delivered to the next-hop ledger, in local-ledger units of that destination
   * ledger.
   *
   * @deprecated // TODO: This interface assumes a fixed quote amount regardless of transfer amount.
   */
  BigInteger getAmount();

  /**
   * The date/time when this quote expires.
   */
  Instant getExpiresAt();

//    route: fullRoute,
//    hop: fullRoute.isLocal ? null : connector,
//    liquidityCurve: fullRoute.curve.shiftX(shiftBy),
//    sourceHoldDuration: request.destinationHoldDuration + fullRoute.minMessageWindow * 1000,
//    expiresAt: Math.min(quoteExpiresAt, fullRoute.curveExpiresAt || Infinity)
//

//    appliesToPrefix = "usd-ledger."
//    expiresAt = 1434412845000
//    hop = null
//    liquidityCurve = LiquidityCurve
//        route = Route
//    sourceHoldDuration = 11001


}
