package ErgasiaProxorimenaThemataDET;

import java.util.ArrayList;
import java.util.*;

public class Main {
    private static double [][] distanceMatrix;
    private static double [][] distanceTimeMatrix;
    private static Node depot = new Node();
    private static ArrayList <Node> allNodes = new ArrayList<>();
    private static ArrayList <Node> customers = new ArrayList<>();
    private static Solution bestSolutionThroughTabuSearch;
    private static Random ran = new Random();

    private final static int minTabuTenure = 1;
    private final static int maxTabuTenure = 20;

    public static void main(String[] args) {
        //-----------
        // erwthma 1
        //-----------

        Main.CreateAllNodesAndCustomerLists(200);

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

        //-----------
        // erwthma 2
        //-----------

        //sort customers based on their load, from higher load to smaller one.

        ArrayList<Route> routes = new ArrayList<>();

        var customersLoad = new ArrayList<>(customers);
        Collections.sort(customersLoad, new Comparator<Node>() {
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

//        test of sorted array
//        System.out.println("Sorted:");
//        for (Node node : customersLoad) {
//            System.out.println(node);
//        }

        RouteBestFit(routes, customersLoad);

//        // for test reasons
//        for (Route route : routes)
//        {
//            System.out.println(route.nodes);
//            System.out.println(route.routeTotalTime);
//            System.out.println(route.load);
//        }

        //-----------
        // erwthma 3
        //-----------

        Solution solution = new Solution();

        ApplyNearestNeighborMethod(solution);
        System.out.println(String.format("Nearest Neighbor Method total km cost: %.3f", solution.cost));
        System.out.println(String.format("Nearest Neighbor Method total time cost (hours): %.3f", solution.timeCost/100));
        System.out.println(String.format("Nearest Neighbor Method total routes: %d", solution.routes.size()));

//        // for test reasons
//        System.out.println("ROUTES");
//        for (Route route : solution.routes)
//        {
//            System.out.println(route.nodes);
//            System.out.println(route.routeTotalTime);
//            System.out.println(route.load);
//        }

        //-----------
        // erwthma 4
        //-----------

        TabuSearch(solution);
        System.out.println(String.format("Tabu search total km cost: %.3f", solution.cost));

//        // for test reasons
//        System.out.println("ROUTES");
//        for (Route route : solution.routes)
//        {
//            System.out.println(route.nodes);
//            System.out.println(route.routeTotalTime);
//            System.out.println(route.load);
//        }
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

    private static void ApplyNearestNeighborMethod(Solution solution) {

        ArrayList<Route> routeList = solution.routes;

        //Q - How many insertions? A - Equal to the number of customers! Thus for i = 0 -> customers.size()
        for (int insertions = 0; insertions < customers.size(); /* the insertions will be updated in the for loop */)
        {
            //A. Insertion Identification
            CustomerInsertion bestInsertion = new CustomerInsertion();
            bestInsertion.cost = Double.MAX_VALUE;
            bestInsertion.timeCost = Constants.truckMaximumAwayTime;
            Route lastRoute = GetLastRoute(routeList);
            if (lastRoute != null)
            {
                IdentifyBestInsertion_NN(bestInsertion, lastRoute);
            }
            //B. Insertion Application
            //Feasible insertion was identified
            if ( bestInsertion.cost < Double.MAX_VALUE && bestInsertion.timeCost < Constants.truckMaximumAwayTime)
            {
                ApplyCustomerInsertion(bestInsertion, solution);
                insertions++;
            }
            //C. If no insertion was feasible
            else
            {
                //C1. There is a customer with demand larger than capacity -> Infeasibility or time added exceeds max time
                if (lastRoute != null && lastRoute.nodes.size() == 2)
                {
                    break;
                }
                else
                {
                    CreateAndPushAnEmptyRouteInTheSolution(solution);
                }
            }
        }
    }

    private static void RouteBestFit(ArrayList<Route> routes, ArrayList<Node> customers) {
        for (Node customer : customers)
        {
            int indexOfBestRoute = -1;
            int minimumEmptySpace = Integer.MAX_VALUE;

            // identify the best fit route ignoring distance between customers
            for (int b = 0; b < routes.size(); b++)
            {
                Route trialRoute = routes.get(b);

                if ((Constants.truckMaxLoad - trialRoute.load) >= customer.demand && trialRoute.routeTotalTime <= Constants.truckMaximumAwayTime)
                {
                    double trialResidualSpace = Constants.truckMaxLoad - (trialRoute.load + customer.demand);

                    if (trialResidualSpace < minimumEmptySpace)
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
                bestFitRoute.nodes.add(customer);
                bestFitRoute.load = bestFitRoute.load + customer.demand;
                bestFitRoute.routeTotalTime = bestFitRoute.nodes.size()*customer.serviceTime;
            }
            else
            {
                Route newRoute = new Route();
                routes.add(newRoute);

                //Assign customer to this new truck
                newRoute.nodes.add(customer);
                newRoute.load = newRoute.load + customer.demand;
                newRoute.routeTotalTime = newRoute.nodes.size()*customer.serviceTime;
            }
        }
    }

    private static Route GetLastRoute(ArrayList<Route> routeList) {
        if (routeList.isEmpty())
            return null;
        return routeList.get(routeList.size()-1);
    }

    private static void IdentifyBestInsertion_NN(CustomerInsertion bestInsertion, Route lastRoute) {
        // The examined node is called candidate
        for (Node candidate : customers)
        {
            // if this candidate has not been pushed in the solution
            if (!candidate.isRouted)
            {
                if (lastRoute.load + candidate.demand <= Route.capacity)
                {
                    ArrayList<Node> nodeSequence = lastRoute.nodes;
                    Node lastCustomerInTheRoute = nodeSequence.get(nodeSequence.size() - 2);

                    double trialCost = distanceMatrix[lastCustomerInTheRoute.ID][candidate.ID];
                    double trialTimeCost = distanceTimeMatrix[lastCustomerInTheRoute.ID][candidate.ID] + candidate.serviceTime;
                    double totalTimeForCandidate = lastRoute.routeTotalTime + distanceTimeMatrix[lastCustomerInTheRoute.ID][candidate.ID] + distanceTimeMatrix[candidate.ID][depot.ID] + candidate.serviceTime - distanceTimeMatrix[lastCustomerInTheRoute.ID][depot.ID];

                    if (trialCost < bestInsertion.cost && totalTimeForCandidate < bestInsertion.timeCost)
                    {
                        bestInsertion.customer = candidate;
                        bestInsertion.insertionRoute = lastRoute;
                        bestInsertion.cost = trialCost;
                        bestInsertion.timeCost = trialTimeCost + candidate.serviceTime;
                    }
                }
            }
        }
    }

    private static void ApplyCustomerInsertion(CustomerInsertion insertion, Solution solution) {
        Node insertedCustomer = insertion.customer;
        Route route = insertion.insertionRoute;

        route.nodes.add(route.nodes.size() - 1, insertedCustomer);

        Node beforeInserted = route.nodes.get(route.nodes.size() - 3);

        double costAdded = distanceMatrix[beforeInserted.ID][insertedCustomer.ID] + distanceMatrix[insertedCustomer.ID][depot.ID];
        double costTimeAdded = distanceTimeMatrix[beforeInserted.ID][insertedCustomer.ID] + distanceTimeMatrix[insertedCustomer.ID][depot.ID] + insertedCustomer.serviceTime;
        double costRemoved = distanceMatrix[beforeInserted.ID][depot.ID];
        double costTimeRemoved = distanceTimeMatrix[beforeInserted.ID][depot.ID];

        route.cost = route.cost + (costAdded - costRemoved);
        route.routeTotalTime = route.routeTotalTime + (costTimeAdded - costTimeRemoved);
        route.load = route.load + insertedCustomer.demand;
        solution.cost = solution.cost + (costAdded - costRemoved);
        solution.timeCost = solution.timeCost + (costTimeAdded- costTimeRemoved);

        insertedCustomer.isRouted = true;
    }

    private static void CreateAndPushAnEmptyRouteInTheSolution(Solution currentSolution) {
        Route rt = new Route();
        rt.nodes.add(depot);
        rt.nodes.add(depot);
        currentSolution.routes.add(rt);
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
            rm.moveTimeCost = moveTimeCost;
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
            originRoute.routeTotalTime = originRoute.routeTotalTime + rm.moveTimeCost;
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
}