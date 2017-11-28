package org.interledger.connector.fx;

import org.interledger.connector.routing.Route;
import org.interledger.connector.routing.RoutingTable;

import javax.money.convert.ExchangeRateProvider;

/**
 * Defines how FX calculations should be performed by this connector, delegating to javax.money for FX calculations.
 *
 * Note that this design deviates from the Javascript implementation in that FX calculations are separated from the
 * routing table. This is because it is expected that either ILQP will go away (e.g., ILP3) and thus the routing table
 * will have no concept of a liquidity curve; OR  that FX calculations will be made in real-time during quoting (e.g.,
 * ILP Atomic-mode). However, implementations that wish to implement ILQP would be free to do so, and can still utilize
 * this interface, connected to an extension of {@link RoutingTable} with an extended {@link Route} to accomplish ILQP +
 * ILPv1 type behavior.
 */
public interface FxEngine extends ExchangeRateProvider {

}
