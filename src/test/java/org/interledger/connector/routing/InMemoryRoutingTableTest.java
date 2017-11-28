package org.interledger.connector.routing;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.interledger.InterledgerAddress;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * Unit tests for {@link InMemoryRoutingTable}.
 */
public class InMemoryRoutingTableTest {

  private static final InterledgerAddress TEST1_PREFIX = InterledgerAddress.of("test1.");
  private static final InterledgerAddress GLOBAL_PREFIX = InterledgerAddress.of("g.");

  private static final InterledgerAddress BOB_GLOBAL_ADDRESS = GLOBAL_PREFIX.with("bank.bob");
  private static final InterledgerAddress BOB_TEST_ADDRESS = TEST1_PREFIX.with("bank.bob");

  @Mock
  private InterledgerPrefixMap interledgerPrefixMapMock;

  private InMemoryRoutingTable routingTable;

  @BeforeMethod
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.routingTable = new InMemoryRoutingTable(interledgerPrefixMapMock);
  }

  ////////////////////
  // Test Constructor
  ////////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testNullConstructor() throws Exception {
    try {
      new InMemoryRoutingTable(null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  ////////////////////
  // Test testAddRoute
  ////////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testAddRouteNull() throws Exception {
    try {
      this.routingTable.addRoute(null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testAddRoute() throws Exception {
    final Route route = this.constructTestRoute(GLOBAL_PREFIX);

    this.routingTable.addRoute(route);

    verify(interledgerPrefixMapMock).add(eq(route));
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  ////////////////////
  // Test testRemoveRoute
  ////////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testRemoveRouteNull() throws Exception {
    try {
      this.routingTable.removeRoute(null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testRemoveRoute() throws Exception {
    final Route route = this.constructTestRoute(GLOBAL_PREFIX);

    this.routingTable.removeRoute(route);

    verify(interledgerPrefixMapMock).removeRoute(eq(route));
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  ////////////////////
  // Test removeAllRoutesForTargetPrefix
  ////////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testRemoveRoutesNull() throws Exception {
    try {
      this.routingTable.removeAllRoutesForTargetPrefix(null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testRemoveRoutesWithEmptyTable() throws Exception {
    final Route route = this.constructTestRoute(GLOBAL_PREFIX);

    this.routingTable.removeAllRoutesForTargetPrefix(route.getTargetPrefix());

    verify(interledgerPrefixMapMock).removeAllRoutes(route.getTargetPrefix());
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  @Test
  public void testRemoveRoutesWithNonEmptyTable() throws Exception {
    final Route route1 = this.constructTestRoute(GLOBAL_PREFIX);
    final Route route2 = this.constructTestRoute(GLOBAL_PREFIX.with("foo."));
    this.routingTable.addRoute(route1);
    this.routingTable.addRoute(route2);
    when(interledgerPrefixMapMock.removeAllRoutes(route1.getTargetPrefix())).thenReturn(ImmutableList.of(route1));
    verify(interledgerPrefixMapMock).add(route1);
    verify(interledgerPrefixMapMock).add(route2);

    this.routingTable.removeAllRoutesForTargetPrefix(route1.getTargetPrefix());

    verify(interledgerPrefixMapMock).removeAllRoutes(route1.getTargetPrefix());
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  @Test
  public void testRemoveRoutes() throws Exception {
    final Route route = this.constructTestRoute(GLOBAL_PREFIX);
    when(interledgerPrefixMapMock.removeAllRoutes(route.getTargetPrefix())).thenReturn(ImmutableList.of(route));
    this.routingTable.addRoute(route);
    verify(interledgerPrefixMapMock).add(route);

    this.routingTable.removeAllRoutesForTargetPrefix(route.getTargetPrefix());

    verify(interledgerPrefixMapMock).removeAllRoutes(route.getTargetPrefix());
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  ////////////////////
  // Test getRoutesByTargetPrefix
  ////////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testGetRoutesNull() throws Exception {
    try {
      this.routingTable.getRoutesByTargetPrefix(null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testGetRoutes() throws Exception {
    final Route route = this.constructTestRoute(GLOBAL_PREFIX);

    this.routingTable.getRoutesByTargetPrefix(route.getTargetPrefix());

    verify(interledgerPrefixMapMock).getRoutes(route.getTargetPrefix());
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  ////////////////////
  // Test forEach
  ////////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testForEachNull() throws Exception {
    try {
      this.routingTable.forEach(null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testForEach() throws Exception {
    final BiConsumer<String, Collection<Route>> action = (o, o2) -> {
    };

    this.routingTable.forEach(action);

    verify(interledgerPrefixMapMock).forEach(action);
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  ////////////////////
  // Test findNextHopRoutes
  ////////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testFindNextHopRouteNull() throws Exception {
    try {
      this.routingTable.findNextHopRoutes(null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testFindNextHopRoute() throws Exception {
    final Route route = this.constructTestRoute(GLOBAL_PREFIX);

    this.routingTable.findNextHopRoutes(BOB_GLOBAL_ADDRESS);

    verify(interledgerPrefixMapMock).findNextHopRoutes(BOB_GLOBAL_ADDRESS);
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  ////////////////////
  // Test findNextHopRoutesWithFilter
  ////////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testFindNextHopRouteWithFilterNull1() throws Exception {
    try {
      InterledgerAddress filter = mock(InterledgerAddress.class);
      this.routingTable.findNextHopRoutes(null, filter);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testFindNextHopRouteWithFilterNull2() throws Exception {
    try {
      InterledgerAddress filter = mock(InterledgerAddress.class);
      this.routingTable.findNextHopRoutes(filter, null);
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testFindNextHopRouteWithFilter() throws Exception {
    // All routes allow this sender...
    final InterledgerAddress usdBarSenderPrefix = GLOBAL_PREFIX.with("usd.bar.");
    // All routes allow this sender...
    final InterledgerAddress usdSenderPrefix = GLOBAL_PREFIX.with("usd.");
    // Depending on the route, this sender is sometimes excluded...
    final InterledgerAddress cnySenderPrefix = GLOBAL_PREFIX.with("cny.");

    final InterledgerAddress finalDestinationAddress = GLOBAL_PREFIX.with("bank.bob");

    // This route is unrestricted...
    final Route allSourcesRoute = this.testRouteBuilder(GLOBAL_PREFIX).build();
    // This route is restricted to all senders whose prefix begins with "g.usd."
    final Route anyUSDSourcesRoute = this.testRouteBuilder(GLOBAL_PREFIX).sourcePrefixRestrictionRegex(Pattern
        .compile("g\\.usd\\.(.*)"))
        .build();
    // This route is restricted to all senders whose prefix begins with "g.usd.bar."
    final Route usdBarOnlySourcesRoute = this.testRouteBuilder(GLOBAL_PREFIX).sourcePrefixRestrictionRegex(Pattern
        .compile("g\\.usd\\.bar\\.(.*)"))
        .build();
    // // This route is restricted to all senders...
    final Route noSourcesRoute = this.testRouteBuilder(GLOBAL_PREFIX).sourcePrefixRestrictionRegex(Pattern
        .compile("a^"))
        .build();

    when(interledgerPrefixMapMock.findNextHopRoutes(finalDestinationAddress))
        .thenReturn(
            Lists.newArrayList(allSourcesRoute, anyUSDSourcesRoute, usdBarOnlySourcesRoute, noSourcesRoute)
        );

    // The USD Bar Sender is eligible for all routes (except the no-senders route), so we expect 3 to be returned...
    Collection<Route> actual = this.routingTable.findNextHopRoutes(finalDestinationAddress, usdBarSenderPrefix);
    assertThat(actual.size(), is(3));
    assertThat(actual.contains(allSourcesRoute), is(true));
    assertThat(actual.contains(anyUSDSourcesRoute), is(true));
    assertThat(actual.contains(usdBarOnlySourcesRoute), is(true));
    assertThat(actual.contains(noSourcesRoute), is(false));

    // The USD Sender is not eligible for the route that requires "g.usd.bar." sources, so we
    // expect only 2 routes to be returned...
    actual = this.routingTable.findNextHopRoutes(finalDestinationAddress, usdSenderPrefix);
    assertThat(actual.size(), is(2));
    assertThat(actual.contains(allSourcesRoute), is(true));
    assertThat(actual.contains(anyUSDSourcesRoute), is(true));
    assertThat(actual.contains(usdBarOnlySourcesRoute), is(false));
    assertThat(actual.contains(noSourcesRoute), is(false));

    // The CNY Sender is eligible for only 1 route, so we expect 1 to be returned...
    actual = this.routingTable.findNextHopRoutes(finalDestinationAddress, cnySenderPrefix);
    assertThat(actual.size(), is(1));
    assertThat(actual.contains(allSourcesRoute), is(true));
    assertThat(actual.contains(anyUSDSourcesRoute), is(false));

    verify(interledgerPrefixMapMock, times(3)).findNextHopRoutes(finalDestinationAddress);
    verifyNoMoreInteractions(interledgerPrefixMapMock);
  }

  ////////////////////
  // Private Helpers
  ////////////////////

  private Route constructTestRoute(final InterledgerAddress targetPrefix) {
    return ImmutableRoute.builder()
        .targetPrefix(targetPrefix)
        .nextHopLedgerAccount(InterledgerAddress.of("g.hub.connie"))
        .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
        .build();
  }

  private ImmutableRoute.Builder testRouteBuilder(final InterledgerAddress targetPrefix) {
    return ImmutableRoute.builder()
        .targetPrefix(targetPrefix)
        .nextHopLedgerAccount(InterledgerAddress.of("g.hub.connie"))
        .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
  }

}