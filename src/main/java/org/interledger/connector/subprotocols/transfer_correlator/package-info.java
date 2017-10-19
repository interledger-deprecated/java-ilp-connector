/**
 * This package contains code to implement the "Transfer Correlator" sub-protocol, which allows an outgoing transfer to
 * store various information (e.g., a source-transfer identifier and source-transfer ledger prefix) that can allow it to
 * be correlated to a source transfer on a different ledger.
 *
 * Connectors use this protocol to avoid having to store information about in a local database about a corresponding
 * source-transfer for each outgoing ledger transfer, which is a performance improvement.
 *
 * Note that this sub-protocol is not appropriate for every use-case, but is instead only appropriate for ledgers that
 *
 *
 * For example, if a connector wanted to take a source-transfer and determine if an existing destination transfer had
 * already been created, then this sub-protocol would not be desirable.
 */
package org.interledger.connector.subprotocols.transfer_correlator;