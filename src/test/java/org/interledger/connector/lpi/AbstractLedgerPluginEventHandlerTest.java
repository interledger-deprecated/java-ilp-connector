package org.interledger.connector.lpi;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.testng.FileAssert.fail;

import org.interledger.InterledgerAddress;
import org.interledger.connector.config.ConnectorConfig;
import org.interledger.connector.config.ConnectorConfigurationService;
import org.interledger.connector.fx.FxEngine;
import org.interledger.connector.routing.PaymentRouter;
import org.interledger.connector.services.LedgerPluginManager;
import org.interledger.plugin.lpi.ImmutableLedgerInfo;
import org.interledger.plugin.lpi.LedgerPlugin;
import org.interledger.plugin.lpi.LedgerPluginTypeId;
import org.interledger.plugin.lpi.MockLedgerPlugin;
import org.interledger.plugin.lpi.MockLedgerPlugin.ExtendedLedgerPluginConfig;
import org.interledger.plugin.lpi.MockLedgerPlugin.SimulatedLedger;
import org.interledger.plugin.lpi.events.IncomingMessgeRequestEvent;
import org.interledger.plugin.lpi.events.LedgerInfoChangedEvent;
import org.interledger.plugin.lpi.events.LedgerPluginConnectedEvent;
import org.interledger.plugin.lpi.events.LedgerPluginDisconnectedEvent;
import org.interledger.plugin.lpi.events.OutgoingMessgeRequestEvent;
import org.interledger.plugin.lpi.events.OutgoingTransferFulfilledEvent;

import com.google.common.collect.ImmutableMap;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

/**
 * Unit tests for {@link AbstractLedgerPluginEventHandler}.
 */
public class AbstractLedgerPluginEventHandlerTest {

  public static final InterledgerAddress LEDGER_PREFIX = InterledgerAddress.of("test1.foo.");

  // TODO: Finish this test!

  private String deterministicIdSecret = "secret";

  @Mock
  private ConnectorConfigurationService connectorConfigurationServiceMock;

  @Mock
  private FxEngine fxEngineMock;

  @Mock
  private LedgerPluginManager ledgerPluginManagerMock;

  @Mock
  private PaymentRouter paymentRouterMock;

  private AbstractLedgerPluginEventHandler abstractLedgerPluginEventHandler;

  @BeforeMethod
  public void setup() {
    MockitoAnnotations.initMocks(this);

    when(connectorConfigurationServiceMock.getConnectorConfig()).thenReturn(this.connectorConfig());

    // Mock the LPI to call constructMockLedgerPlugin for any supplied ILP address...
    doAnswer(invocationOnMock -> Optional.of(this.constructMockLedgerPlugin(invocationOnMock.getArgument(0))))
        .when(ledgerPluginManagerMock).getLedgerPlugin(any());

    this.abstractLedgerPluginEventHandler = new AbstractLedgerPluginEventHandler(
        deterministicIdSecret, connectorConfigurationServiceMock, ledgerPluginManagerMock, paymentRouterMock,
        fxEngineMock
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

  ////////////////
  // Test computeDestinationTransferExpiry
  ////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testComputeDestinationTransferExpiryWithNull() {
    try {
      this.abstractLedgerPluginEventHandler.computeDestinationTransferExpiry(null);
      fail();
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testComputeDestinationTransferExpiry() {
    when(connectorConfigurationServiceMock.getConnectorConfig()).thenReturn(new ConnectorConfig() {
      @Override
      public Duration getTransferExpiryWindow() {
        return Duration.ofSeconds(120);
      }
    });

    final Instant sourceTransferExpiry = Instant.now();
    final Instant actualDestinationTransferExpiry = this.abstractLedgerPluginEventHandler
        .computeDestinationTransferExpiry(sourceTransferExpiry);

    assertThat(actualDestinationTransferExpiry.isBefore(sourceTransferExpiry), is(true));
    assertThat(actualDestinationTransferExpiry, is(sourceTransferExpiry.minus(2, MINUTES)));
  }

  ////////////////
  // Test calculateIlpPacketDestinationAmountWithSlippage
  ////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testCalculateIlpPacketDestinationAmountWithSlippageWithNull1() {
    try {
      this.abstractLedgerPluginEventHandler.calculateIlpPacketDestinationAmountWithSlippage(null, LEDGER_PREFIX);
      fail();
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testCalculateIlpPacketDestinationAmountWithSlippageWithNull2() {
    try {
      this.abstractLedgerPluginEventHandler.calculateIlpPacketDestinationAmountWithSlippage(BigInteger.ZERO, null);
      fail();
    } catch (NullPointerException e) {
      throw e;
    }
  }

  @Test
  public void testCalculateIlpPacketDestinationAmountWithSlippage() {

    BigInteger actual = this.abstractLedgerPluginEventHandler
        .calculateIlpPacketDestinationAmountWithSlippage(new BigInteger("0"), LEDGER_PREFIX);
    assertThat(actual, is(BigInteger.ZERO));

    actual = this.abstractLedgerPluginEventHandler
        .calculateIlpPacketDestinationAmountWithSlippage(new BigInteger("1"), LEDGER_PREFIX);
    assertThat(actual, is(BigInteger.valueOf(1L)));

    actual = this.abstractLedgerPluginEventHandler
        .calculateIlpPacketDestinationAmountWithSlippage(new BigInteger("9999"), LEDGER_PREFIX);
    assertThat(actual, is(BigInteger.valueOf(9989L)));

    actual = this.abstractLedgerPluginEventHandler
        .calculateIlpPacketDestinationAmountWithSlippage(new BigInteger("10000"), LEDGER_PREFIX);
    assertThat(actual, is(BigInteger.valueOf(9990L)));

    actual = this.abstractLedgerPluginEventHandler
        .calculateIlpPacketDestinationAmountWithSlippage(new BigInteger("10001"), LEDGER_PREFIX);
    assertThat(actual, is(BigInteger.valueOf(9991L)));

    // 12 digit number...
    actual = this.abstractLedgerPluginEventHandler
        .calculateIlpPacketDestinationAmountWithSlippage(new BigInteger("999999999999"), LEDGER_PREFIX);
    assertThat(actual, is(BigInteger.valueOf(998999999999L)));

    // 13 digit number...
    actual = this.abstractLedgerPluginEventHandler
        .calculateIlpPacketDestinationAmountWithSlippage(new BigInteger("1000000000000"), LEDGER_PREFIX);
    assertThat(actual, is(BigInteger.valueOf(999000000000L)));

    // 13 digit number...
    actual = this.abstractLedgerPluginEventHandler
        .calculateIlpPacketDestinationAmountWithSlippage(new BigInteger("1000000000001"), LEDGER_PREFIX);
    assertThat(actual, is(BigInteger.valueOf(999000000001L)));

    // 14 digit number...
    actual = this.abstractLedgerPluginEventHandler
        .calculateIlpPacketDestinationAmountWithSlippage(new BigInteger("99999999999999"), LEDGER_PREFIX);
    assertThat(actual, is(BigInteger.valueOf(99899999999999L)));

    // 15 digit number...
    actual = this.abstractLedgerPluginEventHandler
        .calculateIlpPacketDestinationAmountWithSlippage(new BigInteger("100000000000000"), LEDGER_PREFIX);
    assertThat(actual, is(BigInteger.valueOf(99900000000000L)));

    // 15 digit number...
    actual = this.abstractLedgerPluginEventHandler
        .calculateIlpPacketDestinationAmountWithSlippage(new BigInteger("100000000000001"), LEDGER_PREFIX);
    assertThat(actual, is(BigInteger.valueOf(99900000000001L)));
  }

  //////////////////
  // Private Helpers
  //////////////////

  private ConnectorConfig connectorConfig() {
    return new ConnectorConfig() {
    };
  }

  private LedgerPlugin constructMockLedgerPlugin(final InterledgerAddress ledgerPrefix) {
    InterledgerAddress.requireAddressPrefix(ledgerPrefix);
    final ImmutableLedgerInfo ledgerInfo = ImmutableLedgerInfo.builder()
        .currencyScale(2)
        .currencyUnit(Monetary.getCurrency("USD"))
        .ledgerPrefix(ledgerPrefix)
        .build();
    final SimulatedLedger simulatedLedger = new SimulatedLedger(ledgerInfo);

    // Initialize the ledger plugin under test...
    return new MockLedgerPlugin(getLedgerPluginConfig(ledgerPrefix), simulatedLedger);
  }

  protected ExtendedLedgerPluginConfig getLedgerPluginConfig(final InterledgerAddress ledgerPrefix) {
    return new ExtendedLedgerPluginConfig() {

      @Override
      public LedgerPluginTypeId getLedgerPluginTypeId() {
        return LedgerPluginTypeId.of("ilp-plugin-mock");
      }

      @Override
      public InterledgerAddress getLedgerPrefix() {
        return ledgerPrefix;
      }

      @Override
      public InterledgerAddress getConnectorAccount() {
        return ledgerPrefix.with("connector");
      }

      @Override
      public CurrencyUnit getExpectedCurrencyUnit() {
        return Monetary.getCurrency("USD");
      }

      @Override
      public Map<String, String> getOptions() {
        return ImmutableMap.of();
      }

      @Override
      public String getPassword() {
        return "password";
      }
    };
  }
}