package ErgasiaProxorimenaThemataDET;

class Constants {
    private static final double timePiecePerHour = 100; // we consider an hour to be 100 time pieces
    private static final double truckSpeedPerHour = 35; // 35 km per 100 time pieces
    private static final double truckSpeedPerTimePiece = truckSpeedPerHour/timePiecePerHour;
    static final double truckTimePieceSpendPerKilometer = 1/truckSpeedPerTimePiece;
    static final double truckMaximumAwayTime = 5*100; // 1 hour = 1 time piece
    static final double truckMaxLoad = 1500;
}
