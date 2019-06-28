package ErgasiaProxorimenaThemataDET;

class Node {
    int x;
    int y;
    double demand;
    double serviceTime = 0;
    int ID;
    boolean isRouted;
    int tabuIterator;

    @Override
    public String toString() {
//        return String.format("x: %d, \ty: %d, \tdemand: %f, \tID: %d, \tservice time: %.2f", x, y, demand, ID, serviceTime);
        return String.format("ID: %d", ID);
    }
}
