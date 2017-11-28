package org.interledger.connector.routing;

import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

import org.immutables.value.Value;
import org.immutables.value.Value.Default;

import org.interledger.InterledgerAddress;

/**
 * An entry meant to be contained in a {@link RoutingTable}, and used by an Interleder node to determine the "next hop"
 * that a payment should be forwarded along to complete an ILP payment. <p> For more details about the structure of this
 * class as it relates to other routes in a routing table, reference {@link RoutingTable}.
 */
public interface Route {

  //TODO: Liquidity Curve?
  // additionalInfo = undefined
  // minMessageWindow = 1

  /**
   * An {@link InterledgerAddress} used to perform a longest-prefix-match operation against a final payment destination
   * address. For example, if a payment is destined for <tt>g.example.alice</tt>, then the longest-matching target
   * prefix would be <tt>g.example.</tt>, assuming it existed in the routing table.
   *
   * @return An {@link InterledgerAddress}.
   */
  InterledgerAddress getTargetPrefix();

  /**
   * An {@link InterledgerAddress} representing the account that should be listed as the recipient of any next-hop
   * ledger transfers.
   *
   * @return An {@link InterledgerAddress}.
   */
  InterledgerAddress getNextHopLedgerAccount();

  /**
   * An {@link InterledgerAddress} representing the ILP ledger prefix that the next-hop ledger transfer should be made
   * on. Note that, in general, this value should be derived from {@link #getNextHopLedgerAccount()} since the connector
   * account should match the prefix of the ledger it lives in.
   *
   * @return An {@link InterledgerAddress}.
   */
  default InterledgerAddress getNextHopLedgerPrefix() {
    return InterledgerAddress.requireNotAddressPrefix(getNextHopLedgerAccount()).getPrefix();
  }

  /**
   * A regular expression that can restrict routing table destinations to a subset of allowed payment-source prefixes .
   * By default, this filter allows all sources.
   *
   * @return A {@link Pattern}
   */
  Pattern getSourcePrefixRestrictionRegex();

  /**
   * An optionally-present expiration date/time for this route.
   *
   * @return An {@link Instant} representing the
   */
  Optional<Instant> getExpiresAt();

  /**
   * An abstract implementation of {@link Route} for usage by Immutables.
   *
   * @see "https://immutables.github.io"
   */
  @Value.Immutable
  abstract class AbstractRoute implements Route {

    @Default
    public Pattern getSourcePrefixRestrictionRegex() {
      // Default to allow all sources.
      return Pattern.compile("(.*?)");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ImmutableRoute that = (ImmutableRoute) o;

      if (!getTargetPrefix().equals(that.getTargetPrefix())) {
        return false;
      }
      if (!getNextHopLedgerAccount().equals(that.getNextHopLedgerAccount())) {
        return false;
      }

      // Pattern defers to Object.equals, so need to check the pattern() to gauge equality...
      if (!getSourcePrefixRestrictionRegex().pattern().equals(that.getSourcePrefixRestrictionRegex().pattern())) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = getTargetPrefix().hashCode();
      result = 31 * result + getNextHopLedgerAccount().hashCode();
      result = 31 * result + getSourcePrefixRestrictionRegex().pattern().hashCode();
      return result;
    }

    @Value.Check
    void check() {
      InterledgerAddress.requireAddressPrefix(getTargetPrefix());
      InterledgerAddress.requireNotAddressPrefix(getNextHopLedgerAccount());
    }
  }
}
