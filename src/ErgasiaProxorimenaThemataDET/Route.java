package ErgasiaProxorimenaThemataDET;

import java.util.ArrayList;

class Route {
    ArrayList<Node> nodes = new ArrayList<>();
    double cost = 0;
    double load = 0;
    double routeTotalTime = 0;
    static final double capacity = Constants.truckMaxLoad;

    public static double calculateRouteTime(Route route) {
        double timeCost = 0;
        for (int i = 0; i < route.nodes.size() - 1; i++)
            timeCost += Edge.timeDistanceBetweenTwoNodes(Edge.distanceBetweenTwoNodes(route.nodes.get(i), route.nodes.get(i + 1)));
        for (int i = 0; i < route.nodes.size(); i++)
            timeCost += route.nodes.get(i).serviceTime;
        return timeCost;
    }

    @Override
    public String toString() {
        String routeString = "";
        if (nodes.size() > 0) {
            for (Node node : nodes)
                routeString += " -> " + node.ID;
        }
        return routeString + routeTotalTime;
    }
}
