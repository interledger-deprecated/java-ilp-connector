package org.interledger.connector.lpi;

import static org.testng.FileAssert.fail;

import org.interledger.connector.routing.PaymentRouter;
import org.interledger.connector.services.LedgerPluginManager;
import org.interledger.plugin.lpi.events.IncomingMessgeRequestEvent;
import org.interledger.plugin.lpi.events.LedgerInfoChangedEvent;
import org.interledger.plugin.lpi.events.LedgerPluginConnectedEvent;
import org.interledger.plugin.lpi.events.LedgerPluginDisconnectedEvent;
import org.interledger.plugin.lpi.events.OutgoingMessgeRequestEvent;
import org.interledger.plugin.lpi.events.OutgoingTransferFulfilledEvent;

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link AbstractLedgerPluginEventHandler}.
 */
public class AbstractLedgerPluginEventHandlerTest {

  // TODO: Finish this test!

  private String deterministicIdSecret = "secret";

  @Mock
  private LedgerPluginManager ledgerPluginManagerMock;

  @Mock
  private PaymentRouter paymentRouterMock;

  private AbstractLedgerPluginEventHandler abstractLedgerPluginEventHandler;

  @BeforeMethod
  public void setup() {
    this.abstractLedgerPluginEventHandler = new AbstractLedgerPluginEventHandler(
        deterministicIdSecret, ledgerPluginManagerMock, paymentRouterMock
    ) {
      @Override
      public void onLedgerInfoChanged(LedgerInfoChangedEvent event) {

      }

      @Override
      public void onConnect(LedgerPluginConnectedEvent event) {

      }

      @Override
      public void onDisconnect(LedgerPluginDisconnectedEvent event) {

      }

      @Override
      public void onMessageRequest(IncomingMessgeRequestEvent event) {

      }

      @Override
      public void onMessageRequest(OutgoingMessgeRequestEvent event) {

      }

      @Override
      public void onTransferFulfilled(OutgoingTransferFulfilledEvent event) {

      }
    };
  }

  @Test(enabled = false)
  public void testOnError() throws Exception {
    fail();
  }

  @Test(enabled = false)
  public void testOnTransferPrepared() throws Exception {
    fail();
  }

  @Test(enabled = false)
  public void testBuildNextHopTransfer() throws Exception {
    fail();
  }

  @Test(enabled = false)
  public void testOnTransferFulfilled() throws Exception {
    fail();
  }

  @Test(enabled = false)
  public void testOnTransferCancelled() throws Exception {
    fail();
  }

  @Test(enabled = false)
  public void testOnTransferRejected() throws Exception {
    fail();
  }

  @Test(enabled = false)
  public void testOnTransferPrepared1() throws Exception {
    fail();
  }

  @Test(enabled = false)
  public void testOnTransferCancelled1() throws Exception {
    fail();
  }

  @Test(enabled = false)
  public void testOnTransferRejected1() throws Exception {
    fail();
  }

  @Test(enabled = false)
  public void testPrepareDestinationTransfer() throws Exception {
    fail();
  }

  @Test(enabled = false)
  public void testFromLedgerPluginException() throws Exception {
    fail();
  }

  @Test(enabled = false)
  public void testRejectSourceTransferForDestination() throws Exception {
    fail();
  }

  @Test(enabled = false)
  public void testGetLedgerPluginManager() throws Exception {
    fail();
  }

  @Test(enabled = false)
  public void testGetPaymentRouter() throws Exception {
    fail();
  }

}