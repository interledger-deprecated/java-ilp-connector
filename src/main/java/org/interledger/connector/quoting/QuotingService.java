package org.interledger.connector.quoting;

import org.interledger.InterledgerAddress;
import org.interledger.connector.routing.InterledgerHop;

import java.math.BigInteger;

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
   * @param sourceLedgerPrefix - an {@link InterledgerAddress}
   * @param sourceAmount - a BigInteger
   * @return {@link Quote}
   */
  Quote getQuoteBySourceAmount(InterledgerAddress sourceLedgerPrefix, BigInteger sourceAmount);

  
  
  /**
   * Gets a quote to deliver the specified {@code destinationAmount} to a destination ledger via Interledger
   *
   * @param sourceLedgerPrefix - a source {@link InterledgerAddress} 
   * @param destinationAmount - BigInteger
   * @return {@link Quote}
   */
  Quote getQuoteByDestinationAmount(InterledgerAddress sourceLedgerPrefix, BigInteger destinationAmount);
  
  //findBestPathForSourceAmount(IlpAddress sourceLedger, IlpAddress destination, BigInteger sourceAmount);
  
  
  
  //findBestPathForSourceAmount(sourceLedger, ilpPacket.account, sourceTransfer.amount)

  
  
  /**
   *
   * @return {@link InterledgerHop}
   */
  InterledgerHop findNextHop();

}
