package ErgasiaProxorimenaThemataDET;

class Edge {
    Node startingNodeID;
    Node endingNodeID;
    double edgeCost;

    public Edge() {}

    public Edge(Node startingNode, Node endingNode) {
        startingNodeID = startingNode;
        endingNodeID = endingNode;
        edgeCost = distanceBetweenTwoNodes(startingNode, endingNode);
    }

    static double distanceBetweenTwoNodes(Node customerA, Node customerB) {
        return Math.sqrt(Math.abs(Math.pow((customerA.x - customerB.x), 2) + Math.pow((customerA.y - customerB.y),2)));
    }

    static double timeDistanceBetweenTwoNodes(double distance) {
        var timePiecePerKilometer = Constants.truckTimePieceSpendPerKilometer;
        return distance*timePiecePerKilometer;
    }
}
