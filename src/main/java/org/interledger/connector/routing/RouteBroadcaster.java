public interface RouteBroadcaster {
    /*
    constructor's parameters:
        - RoutingTables routingTables;
        - Backend backend;
        - Ledgers ledgers;
        - Config config;
     */

    /**
     *
     */
    public void start();

    /**
     *
     */
    public void removeExpireRoutesSoon();

    //public void markLedgersUnreachable(? lostLedgerLinks);

    //public ? currentEpoch();

    /**
     *
     */
    public void broadcastSoon();

    //public ? broadcast();

    //pulbic ? broadcastToLedger(Ledgers adjacentLedger, Route routes, Ledegers unreachableLedger, ? requestFullTable);

    //pulbic ? crawl();

    /**
     *
     * @param {Plugin} plugin
     */
    public void crawLedgerPlugin(Plugin plugin);

    /**
     *
     * @param {IlpAddress} prefix
     * @param {Connector} connector
     */
    public void addPeer(IlpAddress prefix, Connector connector);

    /**
     *
     * @param {IlpAddress} prefix
     */
    public void deeperLedger(IlpAddress prefix);

    //public ? reloadLocalRoutes();

    //public ? getLocalRoutes();

    /**
     *
     * @return {Promise}
     */
    public Promise addConfigRoutes();

    //public Route tradingPairToLocalRoute(? pair)
}
