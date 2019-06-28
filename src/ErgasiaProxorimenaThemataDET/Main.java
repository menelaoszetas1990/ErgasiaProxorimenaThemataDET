package ErgasiaProxorimenaThemataDET;

import java.util.ArrayList;
import java.util.*;

public class Main {
    private static double [][] distanceMatrix;
    private static double [][] distanceTimeMatrix;
    private static Node depot = new Node();
    private static ArrayList <Node> allNodes = new ArrayList<>();
    private static ArrayList <Node> customers = new ArrayList<>();
    private static Solution bestSolutionThroughLocalSearch;
    private static Solution bestSolutionThroughTabuSearch;
    private static Random ran = new Random();

    private final static int minTabuTenure = 1;
    private final static int maxTabuTenure = 20;

    public static void main(String[] args) {
        //-----------
        // erwthma 1
        //-----------

        CreateAllNodesAndCustomerLists(200);
        InitializeDistanceTables();
        Solution solution = new Solution();

        //-----------
        // erwthma 2
        //-----------

        Question2(solution);

        //-----------
        // erwthma 3
        //-----------

        Question3(solution);

        //-----------
        // erwthma 4
        //-----------

        Question4(solution);
    }

    private static void CreateAllNodesAndCustomerLists(int numberOfCustomers) {

        int birthday = 18051983;
        Random ran = new Random(birthday);
        for (int i = 0 ; i < numberOfCustomers; i++)
        {
            Node customer = new Node();
            customer.x = ran.nextInt(100);
            customer.y = ran.nextInt(100);
            customer.demand = 100*(1 + ran.nextInt(5));
            customer.serviceTime = 25;
            customers.add(customer);
        }

        depot = new Node();
        depot.x = 50;
        depot.y = 50;
        depot.demand = 0;
        allNodes.add(depot);

        allNodes.addAll(customers);

        for (int i = 0 ; i < allNodes.size(); i++)
        {
            Node customer = allNodes.get(i);
            customer.ID = i;

//            test reasons
//            System.out.println(customer);
        }
    }

    private static void InitializeDistanceTables() {

        distanceMatrix = new double[allNodes.size()][allNodes.size()];
        distanceTimeMatrix = new double[allNodes.size()][allNodes.size()];

        for(int i = 0; i < allNodes.size(); i++) {
            for(int j = 0; j < allNodes.size(); j++) {
                distanceMatrix[i][j] = Edge.distanceBetweenTwoNodes(allNodes.get(i), allNodes.get(j));
                distanceTimeMatrix[i][j] = Edge.timeDistanceBetweenTwoNodes(distanceMatrix[i][j]);
            }
        }

//        test reasons
//        for(int i = 0; i < allNodes.size(); i++) {
//            for(int j = 0; j < allNodes.size(); j++) {
//                System.out.print(distanceMatrix[i][j] + "\t");
//            }
//            System.out.println();
//        }
//        for(int i = 0; i < allNodes.size(); i++) {
//            for(int j = 0; j < allNodes.size(); j++) {
//                System.out.print(distanceTimeMatrix[i][j] + "\t");
//            }
//            System.out.println();
//        }
    }

    // ---------
    // Erwthma 2
    // ---------

    private static void Question2(Solution solution) {
        //sort customers based on their load, from higher load to smaller one.

        var customersLoad = SortCustomersByLoad();
        RouteBestFit(solution.routes, customersLoad);

        for (Route route: solution.routes) {
            solution.cost += route.cost;
            solution.timeCost += route.routeTotalTime;
        }

        System.out.println(String.format("Question 2 - same as Bin Packing algorithm total km cost: %.3f", solution.cost));
        System.out.println(String.format("Question 2 - same as Bin packing algorithm total routes: %d", solution.routes.size()));

//        // for test reasons
//        for (Route route : solution.routes)
//        {
//            System.out.println(route.nodes);
//            System.out.println(route.routeTotalTime);
//            System.out.println(route.load);
//        }
    }

    private static ArrayList SortCustomersByLoad() {
        var sortedList = new ArrayList<>(customers);
        Collections.sort(sortedList, new Comparator<Node>() {
            @Override
            public int compare(Node customerALoad, Node customerBLoad)
            {
                if (customerALoad.demand > customerBLoad.demand)
                    return -1;
                if (customerALoad.demand < customerBLoad.demand)
                    return 1;
                return 0;
            }
        });

//        //test of sorted array
//        System.out.println("Sorted:");
//        for (Node node : sortedList) {
//            System.out.println(node + " " + node.demand);
//        }

        return sortedList;
    }

    private static void RouteBestFit(ArrayList<Route> routes, ArrayList<Node> customersSorted) {
        for (Node customer : customersSorted)
        {
            int indexOfBestRoute = -1;
            int minimumEmptySpace = Integer.MAX_VALUE;

            for (int b = 0; b < routes.size(); b++)
            {
                Route trialRoute = routes.get(b);

                if ((Constants.truckMaxLoad - trialRoute.load) >= customer.demand)
                {
                    double trialResidualSpace = Constants.truckMaxLoad - (trialRoute.load + customer.demand);
                    double trialLeftTime = Constants.truckMaximumAwayTime - (trialRoute.routeTotalTime - distanceTimeMatrix[trialRoute.nodes.get(trialRoute.nodes.size() -2).ID][depot.ID] + distanceTimeMatrix[trialRoute.nodes.get(trialRoute.nodes.size() -2).ID][customer.ID] + customer.serviceTime +  distanceTimeMatrix[customer.ID][depot.ID]);

                    if (trialResidualSpace < minimumEmptySpace && trialLeftTime >= 0)
                    {
                        minimumEmptySpace = (int) trialResidualSpace;
                        indexOfBestRoute = b;
                    }
                }
            }

            //If a feasible truck was found
            if (indexOfBestRoute != -1)
            {
                Route bestFitRoute = routes.get(indexOfBestRoute);
                bestFitRoute.nodes.add(bestFitRoute.nodes.size() - 1,customer);
                bestFitRoute.load = bestFitRoute.load + customer.demand;
                bestFitRoute.cost = bestFitRoute.cost - distanceMatrix[bestFitRoute.nodes.get(bestFitRoute.nodes.size() -3).ID][depot.ID] + distanceMatrix[bestFitRoute.nodes.get(bestFitRoute.nodes.size() -3).ID][customer.ID] + distanceMatrix[customer.ID][depot.ID];
                bestFitRoute.routeTotalTime = bestFitRoute.routeTotalTime - distanceTimeMatrix[bestFitRoute.nodes.get(bestFitRoute.nodes.size() -3).ID][depot.ID] + distanceTimeMatrix[bestFitRoute.nodes.get(bestFitRoute.nodes.size() -3).ID][customer.ID] + customer.serviceTime + distanceTimeMatrix[customer.ID][depot.ID];
            }
            else
            {
                Route newRoute = new Route();
                routes.add(newRoute);
                newRoute.nodes.add(depot);
                newRoute.nodes.add(depot);

                //Assign customer to this new truck
                newRoute.nodes.add(newRoute.nodes.size() - 1, customer);
                newRoute.load = newRoute.load + customer.demand;
                newRoute.cost = distanceMatrix[depot.ID][customer.ID] + distanceMatrix[customer.ID][depot.ID];
                newRoute.routeTotalTime = distanceTimeMatrix[depot.ID][customer.ID] + customer.serviceTime + distanceTimeMatrix[customer.ID][depot.ID];
            }
        }

//        // test routes
//        for (Route route : routes)
//            System.out.println(route);
    }

    // ---------
    // Erwthma 3
    // ---------

    private static void Question3(Solution solution) {

        LocalSearch(solution);
        CleanSolution(bestSolutionThroughLocalSearch);
        System.out.println(String.format("Question 3 - Local search algorithm total km cost: %.3f", bestSolutionThroughLocalSearch.cost));
        System.out.println(String.format("Question 3 - Local search algorithm total routes: %d", bestSolutionThroughLocalSearch.routes.size()));

//        // for test reasons
//        System.out.println("LOCAL SEARCH ROUTES");
//        for (Route route : bestSolutionThroughLocalSearch.routes)
//        {
//            System.out.println(route.nodes);
//            System.out.println(route.routeTotalTime);
//            System.out.println(route.load);
//        }
    }

    private static void LocalSearch(Solution solution) {
        bestSolutionThroughLocalSearch = cloneSolution(solution);

        RelocationMove rm = new RelocationMove();
        int repeatsTillLocalOptimumReached = 0;

        while (true)
        {
            repeatsTillLocalOptimumReached++;
            rm.moveCost = Double.MAX_VALUE;
            FindBestRelocationMoveLocalSearch(rm, solution);

            if (LocalOptimumHasBeenReachedLocalSearch(rm))
            {
                System.out.println("Local Search algorithm local optimum reached at: " + repeatsTillLocalOptimumReached + " repeats");
                break;
            }

            //Apply move
            ApplyRelocationMoveLocalSearch(rm, solution);

            StoreBestSolutionLocalSearch(solution);
        }
    }

    private static void FindBestRelocationMoveLocalSearch(RelocationMove rm, Solution sol) {
        ArrayList<Route> routes = sol.routes;
        for (int originRouteIndex = 0; originRouteIndex < routes.size(); originRouteIndex++)
        {
            Route rt1 = routes.get(originRouteIndex);
            for (int targetRouteIndex = 0; targetRouteIndex < routes.size(); targetRouteIndex++)
            {
                Route rt2 = routes.get(targetRouteIndex);

                for (int originNodeIndex = 1; originNodeIndex < rt1.nodes.size() - 1; originNodeIndex++)
                {
                    for (int targetNodeIndex = 0; targetNodeIndex < rt2.nodes.size() - 1; targetNodeIndex++)
                    {
                        //Why? No change for the route involved
                        if (originRouteIndex == targetRouteIndex && (targetNodeIndex == originNodeIndex || targetNodeIndex == originNodeIndex - 1))
                        {
                            continue;
                        }

                        Node a = rt1.nodes.get(originNodeIndex - 1);
                        Node b = rt1.nodes.get(originNodeIndex);
                        Node c = rt1.nodes.get(originNodeIndex + 1);

                        Node insPoint1 = rt2.nodes.get(targetNodeIndex);
                        Node insPoint2 = rt2.nodes.get(targetNodeIndex + 1);

                        //capacity constraints
                        if (originRouteIndex != targetRouteIndex)
                        {
                            if (rt2.load + b.demand > rt2.capacity)
                            {
                                continue;
                            }
                        }

                        double costAdded = distanceMatrix[a.ID][c.ID] + distanceMatrix[insPoint1.ID][b.ID] + distanceMatrix[b.ID][insPoint2.ID];
                        double costRemoved = distanceMatrix[a.ID][b.ID] + distanceMatrix[b.ID][c.ID] + distanceMatrix[insPoint1.ID][insPoint2.ID];
                        double moveCost = costAdded - costRemoved;

                        double costChangeOriginRoute = distanceMatrix[a.ID][c.ID] - (distanceMatrix[a.ID][b.ID] + distanceMatrix[b.ID][c.ID]);
                        double costChangeTargetRoute = distanceMatrix[insPoint1.ID][b.ID] + distanceMatrix[b.ID][insPoint2.ID] - distanceMatrix[insPoint1.ID][insPoint2.ID];
                        double totalObjectiveChange = costChangeOriginRoute + costChangeTargetRoute;

                        double moveTimeCost = 0;
                        if (originRouteIndex == targetRouteIndex) {
                            double costTimeAdded = distanceTimeMatrix[a.ID][c.ID] + distanceTimeMatrix[insPoint1.ID][b.ID] + distanceTimeMatrix[b.ID][insPoint2.ID];
                            double costTimeRemoved = distanceTimeMatrix[a.ID][b.ID] + distanceTimeMatrix[b.ID][c.ID] + distanceTimeMatrix[insPoint1.ID][insPoint2.ID];
                            moveTimeCost = costTimeAdded - costTimeRemoved;
                        }
                        if (originRouteIndex != targetRouteIndex) {
                            double costTimeAddedRt2 = distanceTimeMatrix[insPoint1.ID][b.ID] + distanceTimeMatrix[b.ID][insPoint2.ID] + b.serviceTime;
                            double costTimeRemovedRt2 = distanceTimeMatrix[insPoint1.ID][insPoint2.ID];
                            moveTimeCost = costTimeAddedRt2 - costTimeRemovedRt2;
                        }

                        if (rt2.routeTotalTime + moveTimeCost > Constants.truckMaximumAwayTime)
                            continue;

                        StoreBestRelocationMoveLocalSearch(originRouteIndex, targetRouteIndex, originNodeIndex, targetNodeIndex, moveCost, rm);
                    }
                }
            }
        }
    }

    private static void StoreBestRelocationMoveLocalSearch(int originRouteIndex, int targetRouteIndex, int originNodeIndex, int targetNodeIndex, double moveCost, RelocationMove rm) {
        if (moveCost < rm.moveCost)
        {
            rm.originNodePosition = originNodeIndex;
            rm.targetNodePosition = targetNodeIndex;
            rm.targetRoutePosition = targetRouteIndex;
            rm.originRoutePosition = originRouteIndex;

            rm.moveCost = moveCost;
        }
    }

    private static boolean LocalOptimumHasBeenReachedLocalSearch(RelocationMove rm) {
        if (rm.moveCost > -0.00001)
        {
            return true;
        }
        return false;
    }

    private static void ApplyRelocationMoveLocalSearch(RelocationMove rm, Solution sol) {

        if (rm.moveCost == Double.MAX_VALUE)
            return;

        Route originRoute = sol.routes.get(rm.originRoutePosition);
        Route targetRoute = sol.routes.get(rm.targetRoutePosition);

        Node B = originRoute.nodes.get(rm.originNodePosition);

        if (originRoute == targetRoute)
        {
            originRoute.nodes.remove(rm.originNodePosition);
            if (rm.originNodePosition < rm.targetNodePosition)
            {
                targetRoute.nodes.add(rm.targetNodePosition, B);
            }
            else
            {
                targetRoute.nodes.add(rm.targetNodePosition + 1, B);
            }

            originRoute.cost = originRoute.cost + rm.moveCost;
        }
        else
        {
            Node A = originRoute.nodes.get(rm.originNodePosition - 1);
            Node C = originRoute.nodes.get(rm.originNodePosition + 1);

            Node F = targetRoute.nodes.get(rm.targetNodePosition);
            Node G = targetRoute.nodes.get(rm.targetNodePosition + 1);

            double costChangeOrigin = distanceMatrix[A.ID][C.ID] - distanceMatrix[A.ID][B.ID] - distanceMatrix[B.ID][C.ID];
            double costChangeTarget = distanceMatrix[F.ID][B.ID] + distanceMatrix[B.ID][G.ID] - distanceMatrix[F.ID][G.ID];

            originRoute.load = originRoute.load - B.demand;
            targetRoute.load = targetRoute.load + B.demand;

            originRoute.cost = originRoute.cost + costChangeOrigin;
            targetRoute.cost = targetRoute.cost + costChangeTarget;

            originRoute.nodes.remove(rm.originNodePosition);
            targetRoute.nodes.add(rm.targetNodePosition + 1, B);

            double newMoveCost = costChangeOrigin + costChangeTarget;
        }
        sol.cost = sol.cost + rm.moveCost;
        originRoute.routeTotalTime = Route.calculateRouteTime(originRoute);
        targetRoute.routeTotalTime = Route.calculateRouteTime(targetRoute);
    }

    private static void StoreBestSolutionLocalSearch(Solution sol) {
        if (sol.cost < bestSolutionThroughLocalSearch.cost)
            bestSolutionThroughLocalSearch = cloneSolution(sol);
    }

    // ---------
    // Erwthma 4
    // ---------

    private static void Question4(Solution solution) {
        TabuSearch(solution);
        CleanSolution(bestSolutionThroughTabuSearch);
        System.out.println(String.format("Question 4 - Tabu search algorithm total km cost: %.3f", bestSolutionThroughTabuSearch.cost));
        System.out.println(String.format("Question 4 - Tabu search algorithm total routes: %d", bestSolutionThroughTabuSearch.routes.size()));

//        // for test reasons
//        System.out.println("TABU SEARCH ROUTES");
//        for (Route route : bestSolutionThroughTabuSearch.routes)
//        {
//            System.out.println(route.nodes);
//            System.out.println(route.routeTotalTime);
//            System.out.println(route.load);
//        }
    }

    private static void TabuSearch(Solution sol) {
        bestSolutionThroughTabuSearch = cloneSolution(sol);

        RelocationMove rm = new RelocationMove();
        SwapMove sm = new SwapMove();

        for (int i = 0 ; i < 10000; i++)
        {
            InitializeOperators(rm, sm);

            int operatorType = DecideOperator();

            //Identify Best Move
            if (operatorType == 0)
                FindBestRelocationMove(i, rm, sol);
            else if(operatorType == 1)
                FindBestSwapMove(i, sm, sol);

            if (LocalOptimumHasBeenReached(operatorType, rm, sm))
                break;

            //Apply move
            ApplyMove(i, operatorType, rm, sm, sol);

            TestSolution(sol);

            StoreBestSolution(sol);

//            System.out.print(i + " " + sol.cost + " " + bestSolutionThroughTabuSearch.cost);
//            System.out.println();
        }
    }

    private static Solution cloneSolution(Solution sol) {
        Solution cloned = new Solution();

        //No need to clone - basic type
        cloned.cost = sol.cost;
        cloned.timeCost = sol.timeCost;

        //Need to clone: ArrayLists are objects
        for (int i = 0 ; i < sol.routes.size(); i++)
        {
            Route rt = sol.routes.get(i);
            Route clonedRoute = cloneRoute(rt);
            cloned.routes.add(clonedRoute);
        }

        return cloned;
    }

    private static Route cloneRoute(Route rt) {
        Route cloned = new Route();
        cloned.cost = rt.cost;
        cloned.routeTotalTime = rt.routeTotalTime;
        cloned.load = rt.load;
        cloned.nodes = new ArrayList<>();
        for (int i = 0 ; i < rt.nodes.size(); i++)
        {
            Node n = rt.nodes.get(i);
            cloned.nodes.add(n);
        }
        return cloned;
    }

    private static void InitializeOperators(RelocationMove rm, SwapMove sm) {
        rm.moveCost = Double.MAX_VALUE;
        sm.moveCost = Double.MAX_VALUE;
    }

    private static int DecideOperator()
    {
        return ran.nextInt(2);
    }

    private static void FindBestRelocationMove(int iterator, RelocationMove rm, Solution sol) {
        ArrayList<Route> routes = sol.routes;
        for (int originRouteIndex = 0; originRouteIndex < routes.size(); originRouteIndex++)
        {
            Route rt1 = routes.get(originRouteIndex);
            for (int targetRouteIndex = 0; targetRouteIndex < routes.size(); targetRouteIndex++)
            {
                Route rt2 = routes.get(targetRouteIndex);
                for (int originNodeIndex = 1; originNodeIndex < rt1.nodes.size() - 1; originNodeIndex++)
                {
                    for (int targetNodeIndex = 0; targetNodeIndex < rt2.nodes.size() - 1; targetNodeIndex++)
                    {
                        //Why? No change for the route involved
                        if (originRouteIndex == targetRouteIndex && (targetNodeIndex == originNodeIndex || targetNodeIndex == originNodeIndex - 1))
                            continue;

                        Node a = rt1.nodes.get(originNodeIndex - 1);
                        Node b = rt1.nodes.get(originNodeIndex);
                        Node c = rt1.nodes.get(originNodeIndex + 1);

                        Node insPoint1 = rt2.nodes.get(targetNodeIndex);
                        Node insPoint2 = rt2.nodes.get(targetNodeIndex + 1);

                        //capacity constraints
                        if (originRouteIndex != targetRouteIndex && rt2.load + b.demand > Constants.truckMaxLoad)
                            continue;

                        double costAdded = distanceMatrix[a.ID][c.ID] + distanceMatrix[insPoint1.ID][b.ID] + distanceMatrix[b.ID][insPoint2.ID];
                        double costRemoved = distanceMatrix[a.ID][b.ID] + distanceMatrix[b.ID][c.ID] + distanceMatrix[insPoint1.ID][insPoint2.ID];
                        double moveCost = costAdded - costRemoved;

                        double costTimeAdded = distanceTimeMatrix[insPoint1.ID][b.ID] + distanceTimeMatrix[b.ID][insPoint2.ID] + b.serviceTime;
                        double costTimeRemoved = distanceTimeMatrix[insPoint1.ID][insPoint2.ID];
                        double moveTimeCost = costTimeAdded - costTimeRemoved;

                        if (rt2.routeTotalTime + moveTimeCost > Constants.truckMaximumAwayTime)
                            continue;

                        if (MoveIsTabu(b, iterator, sol, moveCost))
                            continue;

                        StoreBestRelocationMove(originRouteIndex, targetRouteIndex, originNodeIndex, targetNodeIndex, moveCost, moveTimeCost, rm);
                    }
                }
            }
        }
    }

    private static boolean MoveIsTabu(Node n, int iterator, Solution sol, double moveCost) {
        //return false;
        if (Math.abs(moveCost) < 0.0001)
            return true;

        if (moveCost + sol.cost < bestSolutionThroughTabuSearch.cost)
            return false;

        if (iterator < n.tabuIterator)
            return true;

        return false;
    }

    private static void StoreBestRelocationMove(int originRouteIndex, int targetRouteIndex, int originNodeIndex, int targetNodeIndex, double moveCost, double moveTimeCost, RelocationMove rm) {
        if (moveCost < rm.moveCost)
        {
            rm.originNodePosition = originNodeIndex;
            rm.targetNodePosition = targetNodeIndex;
            rm.targetRoutePosition = targetRouteIndex;
            rm.originRoutePosition = originRouteIndex;

            rm.moveCost = moveCost;
        }
    }

    private static void FindBestSwapMove(int iterator, SwapMove sm, Solution sol) {
        ArrayList<Route> routes = sol.routes;
        for (int firstRouteIndex = 0; firstRouteIndex < routes.size(); firstRouteIndex++)
        {
            Route rt1 = routes.get(firstRouteIndex);
            for (int secondRouteIndex = firstRouteIndex; secondRouteIndex < routes.size(); secondRouteIndex++)
            {
                Route rt2 = routes.get(secondRouteIndex);
                for (int firstNodeIndex = 1; firstNodeIndex < rt1.nodes.size() - 1; firstNodeIndex++)
                {
                    int startOfSecondNodeIndex = 1;
                    if (rt1 == rt2)
                        startOfSecondNodeIndex = firstNodeIndex + 1;
                    for (int secondNodeIndex = startOfSecondNodeIndex; secondNodeIndex < rt2.nodes.size() - 1; secondNodeIndex++)
                    {
                        Node a1 = rt1.nodes.get(firstNodeIndex - 1);
                        Node b1 = rt1.nodes.get(firstNodeIndex);
                        Node c1 = rt1.nodes.get(firstNodeIndex + 1);

                        Node a2 = rt2.nodes.get(secondNodeIndex - 1);
                        Node b2 = rt2.nodes.get(secondNodeIndex);
                        Node c2 = rt2.nodes.get(secondNodeIndex + 1);

                        double moveCost;
                        double moveTimeCost;
                        double moveTimeCostRt1 = 0;
                        double moveTimeCostRt2 = 0;

                        if (rt1 == rt2) // within route
                        {
                            if (firstNodeIndex == secondNodeIndex - 1)
                            {
                                double costRemoved = distanceMatrix[a1.ID][b1.ID] + distanceMatrix[b1.ID][b2.ID] + distanceMatrix[b2.ID][c2.ID];
                                double costAdded = distanceMatrix[a1.ID][b2.ID] + distanceMatrix[b2.ID][b1.ID] + distanceMatrix[b1.ID][c2.ID];
                                double costTimeRemoved = distanceTimeMatrix[a1.ID][b1.ID] + distanceTimeMatrix[b1.ID][b2.ID] + distanceTimeMatrix[b2.ID][c2.ID];
                                double costTimeAdded = distanceTimeMatrix[a1.ID][b2.ID] + distanceTimeMatrix[b2.ID][b1.ID] + distanceTimeMatrix[b1.ID][c2.ID];
                                moveCost = costAdded - costRemoved;
                                moveTimeCost = costTimeAdded - costTimeRemoved;
                            }
                            else
                            {
                                double costRemoved1 = distanceMatrix[a1.ID][b1.ID] + distanceMatrix[b1.ID][c1.ID];
                                double costAdded1 = distanceMatrix[a1.ID][b2.ID] + distanceMatrix[b2.ID][c1.ID];
                                double costTimeRemoved1 = distanceTimeMatrix[a1.ID][b1.ID] + distanceTimeMatrix[b1.ID][c1.ID];
                                double costTimeAdded1 = distanceTimeMatrix[a1.ID][b2.ID] + distanceTimeMatrix[b2.ID][c1.ID];

                                double costRemoved2 = distanceMatrix[a2.ID][b2.ID] + distanceMatrix[b2.ID][c2.ID];
                                double costAdded2 = distanceMatrix[a2.ID][b1.ID] + distanceMatrix[b1.ID][c2.ID];
                                double costTimeRemoved2 = distanceTimeMatrix[a2.ID][b2.ID] + distanceTimeMatrix[b2.ID][c2.ID];
                                double costTimeAdded2 = distanceTimeMatrix[a2.ID][b1.ID] + distanceTimeMatrix[b1.ID][c2.ID];

                                moveCost = costAdded1 + costAdded2 - (costRemoved1 + costRemoved2);
                                moveTimeCost = costTimeAdded1 + costTimeAdded2 - (costTimeRemoved1 + costTimeRemoved2);
                            }


                            if (rt1.routeTotalTime + moveTimeCost > Constants.truckMaximumAwayTime)
                                continue;
                        }
                        else // between routes
                        {
                            //capacity constraints
                            if (rt1.load - b1.demand + b2.demand > Constants.truckMaxLoad)
                                continue;
                            if (rt2.load - b2.demand + b1.demand > Constants.truckMaxLoad)
                                continue;

                            double costRemoved1 = distanceMatrix[a1.ID][b1.ID] + distanceMatrix[b1.ID][c1.ID];
                            double costAdded1 = distanceMatrix[a1.ID][b2.ID] + distanceMatrix[b2.ID][c1.ID];
                            double costTimeRemoved1 = distanceTimeMatrix[a1.ID][b1.ID] + distanceTimeMatrix[b1.ID][c1.ID] + b1.serviceTime;
                            double costTimeAdded1 = distanceTimeMatrix[a1.ID][b2.ID] + distanceTimeMatrix[b2.ID][c1.ID] + b2.serviceTime;

                            double costRemoved2 = distanceMatrix[a2.ID][b2.ID] + distanceMatrix[b2.ID][c2.ID];
                            double costAdded2 = distanceMatrix[a2.ID][b1.ID] + distanceMatrix[b1.ID][c2.ID];
                            double costTimeRemoved2 = distanceTimeMatrix[a2.ID][b2.ID] + distanceTimeMatrix[b2.ID][c2.ID] + b2.serviceTime;
                            double costTimeAdded2 = distanceTimeMatrix[a2.ID][b1.ID] + distanceTimeMatrix[b1.ID][c2.ID] + b1.serviceTime;

                            moveCost = costAdded1 + costAdded2 - (costRemoved1 + costRemoved2);
                            moveTimeCostRt1 = costTimeAdded1 - costTimeRemoved1;
                            moveTimeCostRt2 = costTimeAdded2 - costTimeRemoved2;
                        }

                        // Time constraints
                        if (rt1.routeTotalTime + moveTimeCostRt1 > Constants.truckMaximumAwayTime)
                            continue;
                        if (rt2.routeTotalTime + moveTimeCostRt2 > Constants.truckMaximumAwayTime)
                            continue;

                        if (MoveIsTabu(b1, iterator, sol, moveCost) || MoveIsTabu(b2, iterator, sol, moveCost)) //Some Tabu Policy
                            continue;

                        moveTimeCost = moveTimeCostRt1 = moveTimeCostRt2;
                        StoreBestSwapMove(firstRouteIndex, secondRouteIndex, firstNodeIndex, secondNodeIndex, moveCost, moveTimeCost, sm);
                    }
                }
            }
        }
    }

    private static void StoreBestSwapMove(int firstRouteIndex, int secondRouteIndex, int firstNodeIndex, int secondNodeIndex, double moveCost, double moveTimeCost,  SwapMove sm) {
        if (moveCost < sm.moveCost)
        {
            sm.firstRoutePosition = firstRouteIndex;
            sm.firstNodePosition = firstNodeIndex;
            sm.secondRoutePosition = secondRouteIndex;
            sm.secondNodePosition = secondNodeIndex;
            sm.moveCost = moveCost;
            sm.moveTimeCost = moveTimeCost;
        }
    }

    private static boolean LocalOptimumHasBeenReached(int operatorType, RelocationMove rm, SwapMove sm) {
        if ((operatorType == 0 && rm.moveCost > -0.00001) || (operatorType == 1 && sm.moveCost > 0.00001))
            return true;

        return false;
    }

    private static void ApplyMove(int iterator, int operatorType, RelocationMove rm, SwapMove sm, Solution sol) {
        if (operatorType == 0)
            ApplyRelocationMove(iterator, rm, sol);
        else if (operatorType == 1)
            ApplySwapMove(iterator, sm, sol);
    }

    private static void ApplyRelocationMove(int iterator, RelocationMove rm, Solution sol) {
        if (rm.moveCost == Double.MAX_VALUE)
            return;

        Route originRoute = sol.routes.get(rm.originRoutePosition);
        Route targetRoute = sol.routes.get(rm.targetRoutePosition);

        Node B = originRoute.nodes.get(rm.originNodePosition);

        SetTabuIterator(B, iterator);

        if (originRoute == targetRoute)
        {
            originRoute.nodes.remove(rm.originNodePosition);
            if (rm.originNodePosition < rm.targetNodePosition)
                targetRoute.nodes.add(rm.targetNodePosition, B);
            else
                targetRoute.nodes.add(rm.targetNodePosition + 1, B);

            originRoute.cost = originRoute.cost + rm.moveCost;
        }
        else
        {
            Node A = originRoute.nodes.get(rm.originNodePosition - 1);
            Node C = originRoute.nodes.get(rm.originNodePosition + 1);

            Node F = targetRoute.nodes.get(rm.targetNodePosition);
            Node G = targetRoute.nodes.get(rm.targetNodePosition + 1);

            double costChangeOrigin = distanceMatrix[A.ID][C.ID] - distanceMatrix[A.ID][B.ID] - distanceMatrix[B.ID][C.ID];
            double costChangeTarget = distanceMatrix[F.ID][B.ID] + distanceMatrix[B.ID][G.ID] - distanceMatrix[F.ID][G.ID];
            double costTimeChangeOrigin = distanceTimeMatrix[A.ID][C.ID] - distanceTimeMatrix[A.ID][B.ID] - distanceTimeMatrix[B.ID][C.ID] - B.serviceTime;
            double costTimeChangeTarget = distanceTimeMatrix[F.ID][B.ID] + distanceTimeMatrix[B.ID][G.ID] - distanceTimeMatrix[F.ID][G.ID] + B.serviceTime;

            originRoute.load = originRoute.load - B.demand;
            targetRoute.load = targetRoute.load + B.demand;

            originRoute.cost = originRoute.cost + costChangeOrigin;
            targetRoute.cost = targetRoute.cost + costChangeTarget;
            originRoute.routeTotalTime = originRoute.routeTotalTime + costTimeChangeOrigin;
            targetRoute.routeTotalTime = targetRoute.routeTotalTime + costTimeChangeTarget;

            originRoute.nodes.remove(rm.originNodePosition);
            targetRoute.nodes.add(rm.targetNodePosition + 1, B);
        }

        sol.cost = sol.cost + rm.moveCost;
        originRoute.routeTotalTime = Route.calculateRouteTime(originRoute);
        targetRoute.routeTotalTime = Route.calculateRouteTime(targetRoute);
    }

    private static void SetTabuIterator(Node n, int iterator) {
        n.tabuIterator = iterator + minTabuTenure + ran.nextInt(maxTabuTenure - minTabuTenure);
    }

    private static void ApplySwapMove(int iterator, SwapMove sm, Solution sol) {
        if (sm.moveCost == Double.MAX_VALUE)
            return;

        Route firstRoute = sol.routes.get(sm.firstRoutePosition);
        Route secondRoute = sol.routes.get(sm.secondRoutePosition);

        if (firstRoute == secondRoute)
        {
            if (sm.firstNodePosition == sm.secondNodePosition - 1)
            {
                Node A = firstRoute.nodes.get(sm.firstNodePosition);
                Node B = firstRoute.nodes.get(sm.firstNodePosition + 1);

                SetTabuIterator(A, iterator);
                SetTabuIterator(B, iterator);

                firstRoute.nodes.set(sm.firstNodePosition, B);
                firstRoute.nodes.set(sm.firstNodePosition + 1, A);
            }
            else
            {
                Node A = firstRoute.nodes.get(sm.firstNodePosition);
                Node B = firstRoute.nodes.get(sm.secondNodePosition);

                SetTabuIterator(A, iterator);
                SetTabuIterator(B, iterator);

                firstRoute.nodes.set(sm.firstNodePosition, B);
                firstRoute.nodes.set(sm.secondNodePosition, A);
            }

            firstRoute.cost = firstRoute.cost + sm.moveCost;
            firstRoute.routeTotalTime = firstRoute.routeTotalTime + sm.moveTimeCost;
        }
        else
        {
            Node A = firstRoute.nodes.get(sm.firstNodePosition - 1);
            Node B = firstRoute.nodes.get(sm.firstNodePosition);
            Node C = firstRoute.nodes.get(sm.firstNodePosition + 1);

            Node E = secondRoute.nodes.get(sm.secondNodePosition - 1);
            Node F = secondRoute.nodes.get(sm.secondNodePosition);
            Node G = secondRoute.nodes.get(sm.secondNodePosition + 1);

            double costChangeFirstRoute = distanceMatrix[A.ID][F.ID] + distanceMatrix[F.ID][C.ID] -  distanceMatrix[A.ID][B.ID] - distanceMatrix[B.ID][C.ID];
            double costChangeSecondRoute = distanceMatrix[E.ID][B.ID] + distanceMatrix[B.ID][G.ID] -  distanceMatrix[E.ID][F.ID] - distanceMatrix[F.ID][G.ID];
            double costTimeChangeFirstRoute = distanceTimeMatrix[A.ID][F.ID] + distanceTimeMatrix[F.ID][C.ID] + F.serviceTime - distanceTimeMatrix[A.ID][B.ID] - distanceTimeMatrix[B.ID][C.ID] - B.serviceTime;
            double costTimeChangeSecondRoute = distanceTimeMatrix[E.ID][B.ID] + distanceTimeMatrix[B.ID][G.ID] + B.serviceTime - distanceTimeMatrix[E.ID][F.ID] - distanceTimeMatrix[F.ID][G.ID] - F.serviceTime;

            firstRoute.cost = firstRoute.cost + costChangeFirstRoute;
            secondRoute.cost = secondRoute.cost + costChangeSecondRoute;
            firstRoute.routeTotalTime = firstRoute.routeTotalTime + costTimeChangeFirstRoute;
            secondRoute.routeTotalTime = secondRoute.routeTotalTime + costTimeChangeSecondRoute;

            firstRoute.load = firstRoute.load + F.demand - B.demand;
            secondRoute.load = secondRoute.load + B.demand - F.demand;

            firstRoute.nodes.set(sm.firstNodePosition, F);
            secondRoute.nodes.set(sm.secondNodePosition, B);

            SetTabuIterator(B, iterator);
            SetTabuIterator(F, iterator);
        }

        sol.cost = sol.cost + sm.moveCost;
    }

    private static void TestSolution(Solution solution) {

        double secureSolutionCost = 0;
        for (int i = 0 ; i < solution.routes.size(); i++)
        {
            Route rt = solution.routes.get(i);

            double secureRouteCost = 0;

            for (int j = 0 ; j < rt.nodes.size() - 1; j++)
            {
                Node A = rt.nodes.get(j);
                Node B = rt.nodes.get(j + 1);

                secureRouteCost = secureRouteCost + distanceMatrix[A.ID][B.ID];
            }

            secureSolutionCost = secureSolutionCost + secureRouteCost;
        }
    }

    private static void StoreBestSolution(Solution sol) {
        if (sol.cost < bestSolutionThroughTabuSearch.cost)
            bestSolutionThroughTabuSearch = cloneSolution(sol);
    }

    private static void CleanSolution(Solution sol) {
        for (int i = 0; i < sol.routes.size(); i++) {
            if (sol.routes.get(i).nodes.size() == 2)
                sol.routes.remove(sol.routes.get(i));
        }
    }
}