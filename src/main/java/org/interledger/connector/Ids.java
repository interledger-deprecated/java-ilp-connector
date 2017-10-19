package org.interledger.connector;

import org.interledger.plugin.lpi.Wrapped;
import org.interledger.plugin.lpi.Wrapper;

import org.immutables.value.Value;

import java.util.UUID;

/**
 * Wrapped immutable classes for providing type-safe identifiers.
 */
public class Ids {


  /**
   * A wrapper type that defines a "type" of ledger plugin based upon a unique String. For example,
   * "ilp-mock-plugin" or "btp-plugin".
   */
  @Value.Immutable
  @Wrapped
  static abstract class _RouteId extends Wrapper<UUID> {

  }

}
