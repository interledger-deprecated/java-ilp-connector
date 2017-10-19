package org.interledger.connector.quoting;

import java.math.BigInteger;
import org.interledger.InterledgerAddress;
import org.interledger.connector.routing.InterledgerHop;

/**
 * A service for determining FX quotes for particular liquidity paths.
 *
 * A quote contains information relating to the
 */
public interface QuotingService {

    /**
     * Gets a quote to deliver the specified {@code sourceAmount} to a destination ledger via Interledger.
     *
     * A
     *
     * @param sourceLedgerPrefix
     * @param sourceAmount
     */
    Quote getQuoteBySourceAmount(InterledgerAddress sourceLedgerPrefix, BigInteger sourceAmount);

    //findBestPathForSourceAmount(sourceLedger, ilpPacket.account, sourceTransfer.amount)

    /**
     *
     * @return
     */
    InterledgerHop findNextHop();

}
