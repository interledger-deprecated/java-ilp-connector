package org.interledger.connector.subprotocols;

/**
 * Constants for use in various ILP Ledger sub-protocols.
 *
 * @deprecated Move to java-ilp-core.
 */
@Deprecated
public interface SubprotocolConstants {

  String APPLICATION_OCTET_STREAM = "application/octet-stream";

  /**
   * The name of the ILP sub-protocol.
   */
  String ILP_V1 = "ilp/v1";

  /**
   * The name of the transfer-correlator sub-protocol.
   */
  String TRANSFER_CORRELATOR_V1 = "transfer-correlator/v1";
}
