package org.interledger.connector.routing;

import org.interledger.InterledgerAddress;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.plugin.lpi.Transfer;

import java.math.BigInteger;
import java.util.Optional;

/**
 * An interface that determines which payment paths a particular Interledger payment should be
 * routed along using hop-by-hop transfers.
 */
public interface PaymentRouter {

    /**
     * Given a source {@link Transfer} with an embedded Interledger Payment (i.e., a transfer made
     * to this connector on an underlying ledger, also known as an "incoming transfer), determine
     * the "next hop" for an ILP payment according to the routing and quoting requirements of a
     * particular connector implementation.
     *
     * At a general level, this method works as follows:
     *
     * Given an ILP Payment from A→C, find the next hop B on the payment path from A to C. If the
     * next hop is the final one (B == C), return a hop with {@link InterledgerHop#isFinal()} set to
     * {@code true} and information that will direct a connector event-handler to deliver construct
     * a transfer on the final ledger (B/C in this case). Otherwise, return a transfer at B, with an
     * embedded {@link InterledgerPayment} destined for ledger C.
     *
     * @param sourceLedgerPrefix      The {@link InterledgerAddress} prefix of the source ledger
     *                                that an incoming transfer (for this Connector) was received
     *                                on.
     * @param interlederPaymentPacket An {@link InterledgerPayment} containing information about the
     *                                overall ILP payment.
     * @param sourceTransferAmount    The amount of the incoming source transfer.
     */
    default Optional<InterledgerHop> determineNexHop(
        InterledgerAddress sourceLedgerPrefix, InterledgerPayment interlederPaymentPacket,
        BigInteger sourceTransferAmount
    ) {
        return Optional.empty();
    }
}
