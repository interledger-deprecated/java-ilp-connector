package org.interledger.connector.routing;

import java.util.regex.Pattern;

import org.interledger.InterledgerAddress;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.testng.Assert.fail;

/**
 * Unit tests for {@link Route}.
 */
public class RouteTest {

  private static final InterledgerAddress GLOBAL_TARGET_PREFIX = InterledgerAddress.of("g.");
  private static final InterledgerAddress DEFAULT_CONNECTOR_ACCOUNT = InterledgerAddress.of("g.mainhub.connie");
  private static final Pattern ACCEPT_ALL_SOURCES_PATTERN = Pattern.compile("(.*?)");
  private static final Pattern ACCEPT_NO_SOURCES_PATTERN = Pattern.compile("(.*)");

  @Test
  public void testDefaultValues() throws Exception {
    final Route route1 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopLedgerAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    assertThat(route1.getTargetPrefix(), is(GLOBAL_TARGET_PREFIX));
    assertThat(route1.getNextHopLedgerAccount(), is(DEFAULT_CONNECTOR_ACCOUNT));
    assertThat(route1.getExpiresAt().isPresent(), is(false));
    assertThat(route1.getSourcePrefixRestrictionRegex().pattern(), is(ACCEPT_ALL_SOURCES_PATTERN.pattern()));
  }

  @Test
  public void testEqualsHashCode() throws Exception {
    final Route route1 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopLedgerAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();
    final Route route2 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopLedgerAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();

    assertThat(route1, is(route2));
    assertThat(route2, is(route1));
    assertThat(route1.hashCode(), is(route2.hashCode()));
  }

  @Test
  public void testNotEqualsHashCode() throws Exception {
    final Route route1 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopLedgerAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .build();
    final Route route2 = ImmutableRoute.builder()
      .targetPrefix(GLOBAL_TARGET_PREFIX)
      .nextHopLedgerAccount(DEFAULT_CONNECTOR_ACCOUNT)
      .sourcePrefixRestrictionRegex(ACCEPT_NO_SOURCES_PATTERN)
      .build();

    assertThat(route1, is(not(route2)));
    assertThat(route2, is(not(route1)));
    assertThat(route1.hashCode(), is(not(route2.hashCode())));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testTargetAddressNotPrefix() throws Exception {
    try {
      ImmutableRoute.builder()
        .targetPrefix(DEFAULT_CONNECTOR_ACCOUNT)
        .nextHopLedgerAccount(DEFAULT_CONNECTOR_ACCOUNT)
        .build();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(),
        is("InterledgerAddress 'g.mainhub.connie' must be an Address Prefix ending with a dot (.)"));
      throw e;
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNextHopAccountIsPrefix() throws Exception {
    try {
      ImmutableRoute.builder()
        .targetPrefix(GLOBAL_TARGET_PREFIX)
        .nextHopLedgerAccount(GLOBAL_TARGET_PREFIX)
        .build();
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("InterledgerAddress 'g.' must NOT be an Address Prefix ending with a dot (.)"));
      throw e;
    }
  }

}