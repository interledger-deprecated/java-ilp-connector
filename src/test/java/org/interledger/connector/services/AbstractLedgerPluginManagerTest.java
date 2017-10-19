package org.interledger.connector.services;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.FileAssert.fail;

import org.interledger.InterledgerAddress;
import org.interledger.connector.repository.TransferCorrelationRepository;
import org.interledger.connector.subprotocols.SubprotocolDataService;
import org.interledger.plugin.lpi.LedgerPlugin;
import org.interledger.plugin.lpi.LedgerPluginConfig;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link AbstractLedgerPluginManager}.
 */
public class AbstractLedgerPluginManagerTest {

  private static final InterledgerAddress LEDGER_PREFIX1 = InterledgerAddress.of("test1.ledger1.");
  private static final InterledgerAddress LEDGER_PREFIX2 = InterledgerAddress.of("test1.ledger2.");

  @Mock
  private SubprotocolDataService subprotocolDataServiceMock;

  @Mock
  private TransferCorrelationRepository transferCorrelationRepositoryMock;

  private AbstractLedgerPluginManager abstractLedgerPluginManager;

  @BeforeMethod
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.abstractLedgerPluginManager = new AbstractLedgerPluginManager(subprotocolDataServiceMock,
        transferCorrelationRepositoryMock) {
    };
  }

  /////////////////////
  // Constructor Tests
  /////////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testNullConstructor1() throws Exception {
    try {
      new AbstractLedgerPluginManager(null, transferCorrelationRepositoryMock) {
      };
      fail("Should have thrown an exception but did not!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testNullConstructor2() throws Exception {
    try {
      new AbstractLedgerPluginManager(subprotocolDataServiceMock, null) {
      };
      fail("Should have thrown an exception but did not!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  ////////////////////////
  // addLedgerPlugin Tests
  ////////////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testAddLedgerPluginNullLedgerPluginConfig() throws Exception {
    final LedgerPlugin ledgerPluginMock = mock(LedgerPlugin.class);
    try {
      abstractLedgerPluginManager.addLedgerPlugin(null, ledgerPluginMock);
      fail("Should have thrown an exception but did not!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testAddLedgerPluginNullLedgerPlugin() throws Exception {
    final LedgerPluginConfig ledgerPluginConfigMock = mock(LedgerPluginConfig.class);
    try {
      abstractLedgerPluginManager.addLedgerPlugin(ledgerPluginConfigMock, null);
      fail("Should have thrown an exception but did not!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test
  public void testAddLedgerPlugin() throws Exception {
    final LedgerPluginConfig ledgerPluginConfigMock1 = mock(LedgerPluginConfig.class);
    when(ledgerPluginConfigMock1.getLedgerPrefix()).thenReturn(LEDGER_PREFIX1);
    final LedgerPlugin ledgerPluginMock1 = mock(LedgerPlugin.class);

    final LedgerPluginConfig ledgerPluginConfigMock2 = mock(LedgerPluginConfig.class);
    when(ledgerPluginConfigMock2.getLedgerPrefix()).thenReturn(LEDGER_PREFIX2);
    final LedgerPlugin ledgerPluginMock2 = mock(LedgerPlugin.class);

    abstractLedgerPluginManager.addLedgerPlugin(ledgerPluginConfigMock1, ledgerPluginMock1);
    verify(ledgerPluginMock1).connect();
    verify(ledgerPluginMock1, never()).disconnect();
    assertThat(
        abstractLedgerPluginManager.getLedgerPlugin(ledgerPluginConfigMock1.getLedgerPrefix())
            .get(),
        is(ledgerPluginMock1));
    verify(ledgerPluginConfigMock1, times(2)).getLedgerPrefix();
    verifyNoMoreInteractions(ledgerPluginConfigMock1, ledgerPluginMock1);

    abstractLedgerPluginManager.addLedgerPlugin(ledgerPluginConfigMock2, ledgerPluginMock2);
    verify(ledgerPluginMock2).connect();
    verify(ledgerPluginMock2, never()).disconnect();
    assertThat(
        abstractLedgerPluginManager.getLedgerPlugin(ledgerPluginConfigMock2.getLedgerPrefix())
            .get(),
        is(ledgerPluginMock2));
    verify(ledgerPluginConfigMock2, times(2)).getLedgerPrefix();
    verifyNoMoreInteractions(ledgerPluginConfigMock2, ledgerPluginMock2);
  }

  @Test
  public void testAddLedgerPluginWhenAlreadyConnected() throws Exception {
    final LedgerPluginConfig ledgerPluginConfigMock1 = mock(LedgerPluginConfig.class);
    when(ledgerPluginConfigMock1.getLedgerPrefix()).thenReturn(LEDGER_PREFIX1);
    final LedgerPlugin ledgerPluginMock1 = mock(LedgerPlugin.class);

    final LedgerPluginConfig ledgerPluginConfigMock2 = mock(LedgerPluginConfig.class);
    when(ledgerPluginConfigMock2.getLedgerPrefix()).thenReturn(LEDGER_PREFIX2);
    final LedgerPlugin ledgerPluginMock2 = mock(LedgerPlugin.class);

    abstractLedgerPluginManager.addLedgerPlugin(ledgerPluginConfigMock1, ledgerPluginMock1);
    abstractLedgerPluginManager.addLedgerPlugin(ledgerPluginConfigMock1, ledgerPluginMock1);
    verify(ledgerPluginMock1, times(2)).connect();
    verify(ledgerPluginMock1).disconnect();
    assertThat(
        abstractLedgerPluginManager.getLedgerPlugin(ledgerPluginConfigMock1.getLedgerPrefix())
            .get(),
        is(ledgerPluginMock1));
    verify(ledgerPluginConfigMock1, times(3)).getLedgerPrefix();
    verifyNoMoreInteractions(ledgerPluginConfigMock1, ledgerPluginMock1);

    abstractLedgerPluginManager.addLedgerPlugin(ledgerPluginConfigMock2, ledgerPluginMock2);
    abstractLedgerPluginManager.addLedgerPlugin(ledgerPluginConfigMock2, ledgerPluginMock2);
    verify(ledgerPluginMock2, times(2)).connect();
    verify(ledgerPluginMock2).disconnect();
    assertThat(
        abstractLedgerPluginManager.getLedgerPlugin(ledgerPluginConfigMock2.getLedgerPrefix())
            .get(),
        is(ledgerPluginMock2));
    verify(ledgerPluginConfigMock2, times(3)).getLedgerPrefix();
    verifyNoMoreInteractions(ledgerPluginConfigMock2, ledgerPluginMock2);
  }

  ////////////////////////
  // removeLedgerPlugin Tests
  ////////////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testRemoveLedgerPluginNullInput() throws Exception {
    try {
      this.abstractLedgerPluginManager.removeLedgerPlugin(null);
      fail("Should have thrown an exception but did not!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test
  public void testRemoveLedgerPluginWhenNotPresent() throws Exception {
    this.abstractLedgerPluginManager
        .removeLedgerPlugin(InterledgerAddress.of("example.not.there."));
  }

  @Test
  public void testRemoveLedgerPluginWhenPresent() throws Exception {
    final LedgerPluginConfig ledgerPluginConfigMock1 = mock(LedgerPluginConfig.class);
    when(ledgerPluginConfigMock1.getLedgerPrefix()).thenReturn(LEDGER_PREFIX1);
    final LedgerPlugin ledgerPluginMock1 = mock(LedgerPlugin.class);
    abstractLedgerPluginManager.addLedgerPlugin(ledgerPluginConfigMock1, ledgerPluginMock1);

    this.abstractLedgerPluginManager.removeLedgerPlugin(LEDGER_PREFIX1);

    verify(ledgerPluginMock1).disconnect();
    assertThat(abstractLedgerPluginManager.getLedgerPlugin(LEDGER_PREFIX1).isPresent(), is(false));
  }

  ////////////////////////
  // getLedgerPlugin Tests
  ////////////////////////

  @Test(expectedExceptions = NullPointerException.class)
  public void testGetLedgerPluginNullInput() throws Exception {
    try {
      this.abstractLedgerPluginManager.getLedgerPlugin(null);
      fail("Should have thrown an exception but did not!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test
  public void testGetLedgerPluginWhenNotPresent() throws Exception {
    assertThat(abstractLedgerPluginManager.getLedgerPlugin(LEDGER_PREFIX1).isPresent(), is(false));
  }

  @Test
  public void testGetLedgerPluginWhenPresent() throws Exception {
    final LedgerPluginConfig ledgerPluginConfigMock1 = mock(LedgerPluginConfig.class);
    when(ledgerPluginConfigMock1.getLedgerPrefix()).thenReturn(LEDGER_PREFIX1);
    final LedgerPlugin ledgerPluginMock1 = mock(LedgerPlugin.class);
    abstractLedgerPluginManager.addLedgerPlugin(ledgerPluginConfigMock1, ledgerPluginMock1);

    assertThat(abstractLedgerPluginManager.getLedgerPlugin(LEDGER_PREFIX1).isPresent(), is(true));
  }

  ////////////////////////
  // getTransferCorrelationRepository Tests
  ////////////////////////

  @Test
  public void testGetTransferCorrelationRepository() throws Exception {
    assertThat(this.abstractLedgerPluginManager.getTransferCorrelationRepository(),
        is(transferCorrelationRepositoryMock));
  }

  ////////////////////////
  // getSubprotocolDataService Tests
  ////////////////////////

  @Test
  public void testGetSubprotocolDataService() throws Exception {
    assertThat(this.abstractLedgerPluginManager.getSubprotocolDataService(),
        is(subprotocolDataServiceMock));
  }

}