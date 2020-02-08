package gov.nasa.jpf.search.heuristic;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.VM;

public class DelayBoundedSES extends SimplePriorityHeuristic {
    private int delayBudget;

    public DelayBoundedSES (Config conf, VM vm) {
        super(conf, vm);
        this.delayBudget = config.getInt("search.heuristic.DelayBoundedSES.delayBudget", 0);
    }

    @Override
    protected int computeHeuristicValue () {
        int hValue = -vm.getPathLength();
        if (delayBudget > 0) {
            hValue = Integer.MAX_VALUE - delayBudget;
            delayBudget --;
            System.out.println("Delayed state ID: " + vm.getStateId());
        }
        return hValue;
    }
}
