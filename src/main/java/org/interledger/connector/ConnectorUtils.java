package org.interledger.connector;

import org.interledger.InterledgerAddress;
import org.interledger.plugin.lpi.TransferId;

import com.google.common.io.BaseEncoding;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

/**
 * Various utilities that might be useful to consumers of this library.
 */
public class ConnectorUtils {

  /**
   * Using HMAC-SHA-256, deterministically generate a UUID from a secret and a public input, which in this case is a
   * ledger-prefix and a sourceTransferId.
   *
   * This method can be used to generate a deterministic identifier for the "next" transfer that a Connector might make,
   * so that the connector doesn't send duplicate outgoing transfers if it receives duplicate notifications. In the case
   * of a Connector's next-hop transfer identifier, the deterministic generation should ideally be impossible for a
   * third party to predict. Otherwise an attacker might be able to squat on a predicted ID in order to interfere with a
   * payment or make a connector look unreliable. In order to assure this, the connector may use a secret that seeds the
   * deterministic ID generation.
   *
   * @param secret             A {@link String} containing secret information known only to the creator of this transfer
   *                           id.
   * @param sourceLedgerPrefix A {@link InterledgerAddress} containing a ledger prefix for the source ledger that a
   *                           payment came in from. This is necessary in order to correlate the generated outgoing
   *                           payment id to an incoming payment+ledger combination.
   * @param sourceTransferId   A {@link TransferId} that uniquely identifies the source transfer that prompted this
   *                           identifier generation operation. This is necessary in order to correlate the generated
   *                           outgoing payment-id to a single incoming payment+ledger combination.
   *
   * @returns A deterministically generated {@link UUID}.
   **/
  public static TransferId generateDeterministicTransferId(
      final String secret, final InterledgerAddress sourceLedgerPrefix, final TransferId sourceTransferId
  ) {
    Objects.requireNonNull(secret);
    InterledgerAddress.requireAddressPrefix(sourceLedgerPrefix);
    Objects.requireNonNull(sourceTransferId);

    final String publicInput = String.format("%s/%s", sourceLedgerPrefix, sourceTransferId);

    try {
      final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
      messageDigest.update(secret.getBytes());
      byte[] digest = messageDigest.digest(publicInput.getBytes());

      final String hash = BaseEncoding.base16().encode(digest).substring(0, 36);
      final char[] hashCharArray = hash.toCharArray();
      hashCharArray[8] = '-';
      hashCharArray[13] = '-';
      hashCharArray[14] = '4';
      hashCharArray[18] = '-';
      hashCharArray[19] = '8';
      hashCharArray[23] = '-';
      return TransferId.of(UUID.fromString(new String(hashCharArray)));
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

  }
}
