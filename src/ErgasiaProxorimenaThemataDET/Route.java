package ErgasiaProxorimenaThemataDET;

import java.util.ArrayList;

class Route {
    ArrayList<Node> nodes = new ArrayList<>();
    double cost = 0;
    double load = 0;
    double routeTotalTime = 0;
    static final double capacity = Constants.truckMaxLoad;
}
