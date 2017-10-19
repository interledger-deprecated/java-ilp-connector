package org.interledger.connector;


import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.testng.FileAssert.fail;

import org.interledger.InterledgerAddress;
import org.interledger.connector.config.ConnectorConfigurationService;
import org.interledger.connector.services.LedgerPluginManager;
import org.interledger.plugin.lpi.LedgerPlugin;
import org.interledger.plugin.lpi.LedgerPluginConfig;
import org.interledger.plugin.lpi.LedgerPluginTypeId;
import org.interledger.plugin.lpi.handlers.LedgerPluginEventHandler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Objects;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

/**
 * Unit tests for {@link AbstractConnector}.
 */
public class AbstractConnectorTest {

  public static final String EXAMPLE_FOO1 = "example.foo1.";
  public static final String EXAMPLE_FOO2 = "example.foo2.";
  public static final String EXAMPLE_EXTENSION_FOO3 = "example.extension.foo3.";
  @Mock
  private LedgerPlugin ledgerPluginMock;

  @Mock
  private ConnectorConfigurationService connectorConfigurationServiceMock;

  @Mock
  private LedgerPluginEventHandler ledgerPluginEventHandlerMock;

  @Mock
  private LedgerPluginManager ledgerPluginManagerMock;

  private AbstractConnector abstractConnector;

  @BeforeMethod
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testNullConstructorInput1() {
    try {
      new AbstractConnector(
          null, ledgerPluginEventHandlerMock, connectorConfigurationServiceMock
      ) {
        @Override
        protected LedgerPlugin constructLedgerPlugin(LedgerPluginConfig ledgerPluginConfig) {
          return null;
        }
      };
      fail("Should have thrown an exception but did not!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testNullConstructorInput2() {
    try {
      new AbstractConnector(
          ledgerPluginManagerMock, null, connectorConfigurationServiceMock
      ) {
        @Override
        protected LedgerPlugin constructLedgerPlugin(LedgerPluginConfig ledgerPluginConfig) {
          return null;
        }
      };
      fail("Should have thrown an exception but did not!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testNullConstructorInput3() {
    try {
      new AbstractConnector(
          ledgerPluginManagerMock, ledgerPluginEventHandlerMock, null
      ) {
        @Override
        protected LedgerPlugin constructLedgerPlugin(LedgerPluginConfig ledgerPluginConfig) {
          return null;
        }
      };
      fail("Should have thrown an exception but did not!");
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is(nullValue()));
      throw e;
    }
  }

  @Test
  public void testConstructLedgerPlugin() throws Exception {
    this.initializeAbstractConnector();
    assertThat(abstractConnector.constructLedgerPlugin(mock(LedgerPluginConfig.class)),
        is(ledgerPluginMock));
  }

  @Test
  public void testGetLedgerPluginManager() throws Exception {
    this.initializeAbstractConnector();
    assertThat(abstractConnector.getLedgerPluginManager(), is(ledgerPluginManagerMock));
  }

  /**
   * Test with no ledger plugins configured.
   */
  @Test
  public void testInitializeConnectorWithNoLedgerPlugins() {
    when(connectorConfigurationServiceMock.getLedgerPluginConfigurations()).thenReturn(
        ImmutableList.of()
    );

    this.initializeAbstractConnector();

    verifyZeroInteractions(ledgerPluginManagerMock);
  }

  /**
   * Simulate that if {@link AbstractConnector#constructLedgerPlugin} throws an exception for one
   * ledger plugin, then the rest of the plugins are still configured properly.
   */
  @Test
  public void testInitializeConnectorWithOneFailedLedgerPlugin() {
    final TestLedgerPluginConfig ledgerPluginConfig1 = new TestLedgerPluginConfig(
        InterledgerAddress.of(EXAMPLE_FOO1));
    final TestLedgerPluginConfig ledgerPluginConfig2 = new TestLedgerPluginConfig(
        InterledgerAddress.of(EXAMPLE_FOO2));
    final ExtendedTestLedgerPluginConfig ledgerPluginConfig3 = new ExtendedTestLedgerPluginConfig(
        InterledgerAddress.of(EXAMPLE_EXTENSION_FOO3));

    when(connectorConfigurationServiceMock.getLedgerPluginConfigurations()).thenReturn(
        ImmutableList.of(ledgerPluginConfig1, ledgerPluginConfig2, ledgerPluginConfig3)
    );

    this.abstractConnector = new AbstractConnector(
        ledgerPluginManagerMock, ledgerPluginEventHandlerMock, connectorConfigurationServiceMock
    ) {
      @Override
      protected LedgerPlugin constructLedgerPlugin(LedgerPluginConfig ledgerPluginConfig) {
        if (ledgerPluginConfig.getLedgerPrefix().equals(InterledgerAddress.of(EXAMPLE_FOO1))) {
          throw new NullPointerException("Oops, we're simulating that this failed!");
        } else { //if(ledgerPluginConfig.getLedgerPrefix().equals(ledgerPluginConfig2.getLedgerPrefix())){
          return ledgerPluginMock;
        }
      }

      @Override
      public ConnectorConfigurationService getConnectorConfigurationService() {
        return connectorConfigurationServiceMock;
      }
    };

    // Initialization happens by default when the AbstractConnector is constructed, so no need to
    // invoke it here!

    // This verifies that the remaining two non-failed plugins still get added to the plugin manager.
    verify(ledgerPluginManagerMock, times(0)).addLedgerPlugin(eq(ledgerPluginConfig1), any());
    verify(ledgerPluginManagerMock).addLedgerPlugin(eq(ledgerPluginConfig2), any());
    verify(ledgerPluginManagerMock).addLedgerPlugin(eq(ledgerPluginConfig3), any());
    verifyNoMoreInteractions(ledgerPluginManagerMock);
  }

  /**
   * If the Stream handler is not properly setup in {@link AbstractConnector#initializeConnector()},
   * then no ledger plugins will be initialized. This method asserts that is setup properly. It does
   * so by expecting two instances of {@link LedgerPluginConfig} to be present in the overall
   * configuration, but also includes a sub-class to be sure that this is also captured by the
   * filter properly.
   */
  @Test
  public void testInitializeConnector() {
    final TestLedgerPluginConfig ledgerPluginConfig1 = new TestLedgerPluginConfig(
        InterledgerAddress.of(EXAMPLE_FOO1));
    final TestLedgerPluginConfig ledgerPluginConfig2 = new TestLedgerPluginConfig(
        InterledgerAddress.of(EXAMPLE_FOO2));
    final ExtendedTestLedgerPluginConfig ledgerPluginConfig3 = new ExtendedTestLedgerPluginConfig(
        InterledgerAddress.of(EXAMPLE_EXTENSION_FOO3));

    when(connectorConfigurationServiceMock.getLedgerPluginConfigurations()).thenReturn(
        ImmutableList.of(ledgerPluginConfig1, ledgerPluginConfig2, ledgerPluginConfig3)
    );

    this.initializeAbstractConnector();

    verify(ledgerPluginManagerMock).addLedgerPlugin(eq(ledgerPluginConfig1), any());
    verify(ledgerPluginManagerMock).addLedgerPlugin(eq(ledgerPluginConfig2), any());
    verify(ledgerPluginManagerMock).addLedgerPlugin(eq(ledgerPluginConfig3), any());
    verifyNoMoreInteractions(ledgerPluginManagerMock);
  }

  //////////////////
  // Private Helpers
  //////////////////

  /**
   * Helper method to construct an instance of  {@link AbstractConnector} for use in _most_
   * happy-path test in this test class.
   */
  private void initializeAbstractConnector() {
    this.abstractConnector = new AbstractConnector(
        ledgerPluginManagerMock, ledgerPluginEventHandlerMock, connectorConfigurationServiceMock
    ) {
      @Override
      protected LedgerPlugin constructLedgerPlugin(LedgerPluginConfig ledgerPluginConfig) {
        return ledgerPluginMock;
      }

      @Override
      public ConnectorConfigurationService getConnectorConfigurationService() {
        return connectorConfigurationServiceMock;
      }
    };
  }

  /**
   * An extension of {@link LedgerPluginConfig} used to ensure that {@link
   * AbstractConnector#initializeConnector()} functions properly for all instances (including
   * subclasses) of {@link LedgerPluginConfig}.
   */
  private static class TestLedgerPluginConfig implements LedgerPluginConfig {

    private final InterledgerAddress ledgerPrefix;

    private TestLedgerPluginConfig(InterledgerAddress ledgerPrefix) {
      this.ledgerPrefix = Objects.requireNonNull(ledgerPrefix);
    }

    /**
     * The type of this ledger plugin.
     */
    @Override
    public LedgerPluginTypeId getLedgerPluginTypeId() {
      return LedgerPluginTypeId.of("example-ledger-plugin");
    }

    /**
     * The identifying ledger prefix for this plugin.
     */
    @Override
    public InterledgerAddress getLedgerPrefix() {
      return ledgerPrefix;
    }

    /**
     * The connector account on the underlying ledger.
     */
    @Override
    public InterledgerAddress getConnectorAccount() {
      return InterledgerAddress.of("example.foo.connector");
    }

    /**
     * The expected currency-unit for this ledger plugin.
     */
    @Override
    public CurrencyUnit getExpectedCurrencyUnit() {
      return Monetary.getCurrency("USD");
    }

    /**
     * The options for a given ledger plugin.
     */
    @Override
    public Map<String, String> getOptions() {
      return ImmutableMap.of();
    }
  }

  /**
   * An extension of {@link LedgerPluginConfig} used to ensure that {@link
   * AbstractConnector#initializeConnector()} functions properly for all instances (including
   * subclasses) of {@link LedgerPluginConfig}.
   */
  private static class ExtendedTestLedgerPluginConfig extends TestLedgerPluginConfig {

    private ExtendedTestLedgerPluginConfig(InterledgerAddress ledgerPrefix) {
      super(ledgerPrefix);
    }

    /**
     * The type of this ledger plugin.
     */
    public String getSomeValue() {
      return "Foo";
    }

  }

}