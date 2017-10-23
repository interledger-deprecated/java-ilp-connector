public interface RouteBuilder {
    import org.interledger.connector.config.ConnectorConfig;
    import org.interledger.InterledgerAddress;

    //class parameters and constructor declaration
    /*
    private Ledgers ledgers;
    private RoutingTables routingTables;
    private Quoter quoter;
    private Integer minMessageWindow;
    private Integer maxHoldTime;
    private ? slippage;
    private ? secret;
    private ? unwiseUseSameTransferId;


    public RouteBuilder(Ledgers ledgers, Quoter quoter, ConnectorConfig config) {};
    */

    /**
     *
     * @param {IlpPacket} ilpPacket
     * @return {QuoteLiquidityResponse} or can be an HashMap
     */
    public QuoteLiquidityResponse quoteLiquidity(IlpPacket ilpPacket);

    /**
     * get the quote for a specified source amount
     * @param {IlpPacket} ilpPacket
     * @return {QuoteBySourceResponse} or can be an HashMap
     */
    public QuoteBySourceResponse quoteBySource(IlpPacket ilpPacket);

    /**
     * get the quote for a specified destination amount
     * @param {IlpPacket} ilpPacket
     * @return {QuoteByDestinationResponse} or can be an HashMap
     */
    public QuoteByDestinationResponse quoteByDestination(IlpPacket ilpPacket);

    /**
     *
     * @param {Transfer} sourceTransfer
     * @return {Transfer}
     */
    public Transfer getDestinationTransfer(Transfer sourceTransfer);

    /**
     * check if the ledger in parameter is well connected to the connector
     * (should not be use with "this", associated to a class parameter because there is no return value).
     * @param {Ledgers} ledger
     */
    public void verifyLedgerIsConnected(Ledgers ledger);

    /**
     * check the valididty of the source and destination hold duration
     * (should not be use with "this", associated to a class parameter because there is no return value)
     * (propose the use of the class java.time.Duration which is well define for handle duration operations).
     * @param {Duration} sourceHoldDuration
     * @param {Duration} destinationHoldDuration
     */
    public void validateHoldDurations(Duration sourceHoldDuration, Duration destinationHoldDuration);
}
