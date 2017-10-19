package org.interledger.connector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.interledger.InterledgerAddress;
import org.interledger.plugin.lpi.TransferId;

import org.testng.annotations.Test;

import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Unit tests for {@link ConnectorUtils}.
 */
public class ConnectorUtilsTest {

  /**
   * Using the same input, generate a UUID and assert that it is always the same.
   */
  @Test
  public void testGenerateTransferId() throws Exception {

    final String secret = UUID.randomUUID().toString();
    final InterledgerAddress ledgerPrefix = InterledgerAddress.of("test1.foo.bar.");
    final TransferId transferId = TransferId.of(UUID.randomUUID());

    final TransferId expectedUUID = ConnectorUtils
        .generateTransferId(secret, ledgerPrefix, transferId);

    IntStream.of(1000).forEach(i ->
        assertThat(ConnectorUtils.generateTransferId(secret, ledgerPrefix, transferId),
            is(expectedUUID))
    );
  }

}