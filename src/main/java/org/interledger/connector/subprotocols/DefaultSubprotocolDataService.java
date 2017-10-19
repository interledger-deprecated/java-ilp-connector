package org.interledger.connector.subprotocols;

import org.interledger.codecs.CodecContext;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.subprotocols.ImmutableSubprotocolData;
import org.interledger.subprotocols.SubprotocolData;

import java.util.Objects;

/**
 * A default implementation of {@link SubprotocolDataService}.
 *
 * @deprecated This should be merged into java-ilp-core.
 */
@Deprecated
public class DefaultSubprotocolDataService implements SubprotocolDataService {

  private final CodecContext interledgerCodedContext;

  public DefaultSubprotocolDataService(final CodecContext interledgerCodecContext) {
    this.interledgerCodedContext = Objects.requireNonNull(interledgerCodecContext);
  }

  @Override
  public SubprotocolData toProtocolData(final InterledgerPayment interledgerPayment) {
    Objects.requireNonNull(interledgerPayment);

    return ImmutableSubprotocolData.builder()
        .protocolName(SubprotocolConstants.ILP_V1)
        .data(interledgerCodedContext.write(interledgerPayment))
        .dataContentType("application/json")
        .build();
  }

  @Override
  public InterledgerPayment toInterledgerPayment(final SubprotocolData subprotocolData) {
    Objects.requireNonNull(subprotocolData);
    return interledgerCodedContext.read(InterledgerPayment.class, subprotocolData.getData());
  }

//  @Override
//  public SubprotocolData toProtocolData(final TransferCorrelation transferCorrelation) {
//    Objects.requireNonNull(transferCorrelation);
//
//    return ImmutableSubprotocolData.builder()
//        .protocolName(transferCorrelation.getProtocolName())
//        .dataContentType(transferCorrelation.getContentType())
//        .data(interledgerCodedContext.write(transferCorrelation))
//        .build();
//  }
//
//  @Override
//  public TransferCorrelation toTransferCorrelation(final SubprotocolData subprotocolData) {
//    Objects.requireNonNull(subprotocolData);
//
//    return interledgerCodedContext.read(TransferCorrelation.class, subprotocolData.getData());
//  }
}
