package org.interledger.connector.fx;

import org.interledger.InterledgerAddress;
import org.interledger.connector.routing.Route;
import org.interledger.plugin.lpi.Transfer;

import java.math.BigInteger;
import java.util.Objects;

import javax.money.convert.ExchangeRateProvider;

/**
 * Defines how FX calculations should be performed by this connector, delegating to javax.money for FX calculations.
 *
 * Note that this design deviates from the Javascript implementation in that FX calculations are separated from the
 * routing table. This is because it is expected that either ILQP will go away (e.g., ILP3) and thus the routing table
 * will have no concept of a liquidity curve; OR  that FX calculations will be made in real-time during quoting (e.g.,
 * ILP Atomic-mode). However, implementations that wish to implement ILQP would be free to do so, and can still utilize
 * this interface, connected to an extension of {@link org.interledger.connector.routing.RoutingTable} with an extended
 * {@link Route} to accomplish ILQP + ILP1 type behavior.
 */
public interface FxEngine extends ExchangeRateProvider {

  /**
   * Given a source transfer, compute the amount (in local units of the destination ledger) that should be transferred
   * to the next-hop local ledger.
   */
  default BigInteger computeNextHopLocalTransferAmount(
      final Transfer sourceTransfer,
      final InterledgerAddress destinationLedgerPrefix
  ) {
    Objects.requireNonNull(sourceTransfer);
    InterledgerAddress.requireAddressPrefix(destinationLedgerPrefix);













    return null;
  }

}
