package org.interledger.connector.subprotocols;

import org.interledger.ilp.InterledgerPayment;
import org.interledger.subprotocols.SubprotocolData;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A service for interacting with SubProtocol data, including extraction and insertion of custom subprotocol data from
 * various ledger and connector communications payloads.
 *
 * @deprecated TODO: Move to java-ilp-core
 */
@Deprecated
public interface SubprotocolDataService {

  /**
   * Transform an instance of {@link InterledgerPayment} to an instance of {@link SubprotocolData}.
   *
   * @param interledgerPayment An instance of {@link InterledgerPayment}.
   *
   * @return An instance of {@link SubprotocolData}.
   */
  SubprotocolData toProtocolData(final InterledgerPayment interledgerPayment);

  /**
   * Transform an instance of {@link SubprotocolData} to an instance of {@link InterledgerPayment}.
   *
   * @param subprotocolData An instance of {@link SubprotocolData}.
   *
   * @return An instance of {@link InterledgerPayment}.
   */
  InterledgerPayment toInterledgerPayment(final SubprotocolData subprotocolData);

  /**
   * Extract an Interledger payment packet from a list of sub-protocol data objects.
   *
   * @param subprotocolDataList A {@link List} of type {@link SubprotocolData} that contains optionally-present {@link
   *                            InterledgerPayment} as on of the protocol-data entries.
   *
   * @return The optionally-present {@link InterledgerPayment}, or {@link Optional#empty()} .
   */
  default Optional<InterledgerPayment> findInterledgerPayment(
    final List<SubprotocolData> subprotocolDataList) {
    Objects.requireNonNull(subprotocolDataList);

    return subprotocolDataList.stream()
      .filter(pr -> pr.getProtocolName().equalsIgnoreCase(SubprotocolConstants.ILP_V1))
      // Convert to an InterledgerPayment...
      .map(this::toInterledgerPayment)
      .findFirst();
  }

//  /**
//   * Transform an instance of {@link InterledgerPayment} to an instance of {@link SubprotocolData}.
//   *
//   * @param transferCorrelation An instance of {@link TransferCorrelation}.
//   *
//   * @return An instance of {@link SubprotocolData}.
//   */
//  SubprotocolData toProtocolData(final TransferCorrelation transferCorrelation);
//
//  /**
//   * Transform an instance of {@link SubprotocolData} to an instance of {@link InterledgerPayment}.
//   *
//   * @param subprotocolData An instance of {@link SubprotocolData}.
//   *
//   * @return An instance of {@link InterledgerPayment}.
//   */
//  TransferCorrelation toTransferCorrelation(final SubprotocolData subprotocolData);
//
//  /**
//   * Extract an Interledger payment packet from a CLP transfer object.
//   *
//   * @param subprotocolDataList A {@link List} of type {@link SubprotocolData} that contains
//   *                            optionally-present {@link InterledgerPayment} as on of the
//   *                            protocol-data entries.
//   *
//   * @return The optionally-present {@link InterledgerPayment}, or {@link Optional#empty()} .
//   */
//  default Optional<TransferCorrelation> findTransferCorrelation(
//      final List<SubprotocolData> subprotocolDataList
//  ) {
//    Objects.requireNonNull(subprotocolDataList);
//
//    return subprotocolDataList.stream()
//        .filter(pr -> pr.getProtocolName()
//            .equalsIgnoreCase(SubprotocolConstants.TRANSFER_CORRELATOR_V1))
//        // Convert to an InterledgerPayment...
//        .map(this::toTransferCorrelation)
//        .findFirst();
//  }
}