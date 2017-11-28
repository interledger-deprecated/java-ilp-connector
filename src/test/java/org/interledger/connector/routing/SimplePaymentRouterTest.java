package org.interledger.connector.routing;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.interledger.InterledgerAddress;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SimplePaymentRouterTest {

  private static final InterledgerAddress GLOBAL_PREFIX = InterledgerAddress.of("g.");
  private static final InterledgerAddress BANK_A_PREFIX = GLOBAL_PREFIX.with("banka.");
  private static final InterledgerAddress BOB_AT_BANK_B = GLOBAL_PREFIX.with("banka.bob");

  @Mock
  private RoutingTable<Route> routingTableMock;

  private PaymentRouter<Route> paymentRouter;

  @BeforeMethod
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.paymentRouter = new SimplePaymentRouter(routingTableMock);
  }

  @Test
  public void testFindNextHopRoute() throws Exception {
    final Route route1 = mock(Route.class);
    final Route route2 = mock(Route.class);
    when(routingTableMock.findNextHopRoutes(BOB_AT_BANK_B)).thenReturn(ImmutableList.of(route1, route2));

    final Optional<Route> actual = this.paymentRouter.findBestNexHop(BOB_AT_BANK_B);

    assertThat(actual.isPresent(), is(true));
    for (int i = 0; i < 100; i++) {
      //Try this 100 times to make sure we always get what's expected...
      assertThat(actual.get().equals(route1) || actual.get().equals(route2), is(true));
    }
    verify(routingTableMock).findNextHopRoutes(BOB_AT_BANK_B);
    verifyNoMoreInteractions(routingTableMock);
  }

  @Test
  public void testFindNextHopRouteWithFilter() throws Exception {
    final Route route1 = mock(Route.class);
    final Route route2 = mock(Route.class);
    when(routingTableMock.findNextHopRoutes(BOB_AT_BANK_B, BANK_A_PREFIX))
      .thenReturn(ImmutableList.of(route1, route2));

    Optional<Route> actual = this.paymentRouter.findBestNexHop(BOB_AT_BANK_B, BANK_A_PREFIX);

    assertThat(actual.isPresent(), is(true));
    for (int i = 0; i < 100; i++) {
      //Try this 100 times to make sure we always get what's expected...
      assertThat(actual.get().equals(route1) || actual.get().equals(route2), is(true));
    }
    verify(routingTableMock).findNextHopRoutes(BOB_AT_BANK_B, BANK_A_PREFIX);
    verifyNoMoreInteractions(routingTableMock);
  }

}