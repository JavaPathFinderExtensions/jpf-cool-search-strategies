package gov.nasa.jpf.search;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.VM;

import java.util.HashMap;

public class DelayBoundedDFS extends Search {
    private int delayBudget;
    private HashMap<Integer, Integer> exploredChoiceMap;  // key: stateID, value: # of choices explored for the state

    public DelayBoundedDFS(Config conf, VM vm) {
        super(conf, vm);
        this.delayBudget = config.getInt("search.DelayBoundedDFS.delayBudget", 0);
        this.exploredChoiceMap = new HashMap<>();

    }

    @Override
    public boolean requestBacktrack () {
        doBacktrack = true;

        return true;
    }

    /**
     * Adapted from DFS searach
     */
    @Override
    public void search () {
        boolean depthLimitReached = false;
        int prevStateID = -1;
        depth = 0;

        notifySearchStarted();

        while (!done) {
            if (checkAndResetBacktrackRequest() || !isNewState() || isEndState() || isIgnoredState() || depthLimitReached ) {


                if (!backtrack()) { // backtrack not possible, done
                    break;
                }
                System.out.println("backtracked to stateID: " + vm.getStateId());
                if (exploredChoiceMap.get(vm.getStateId()) > 1) {
                    // since we backtracked, we get back one delay budget spent on exploring prev state
                    delayBudget++;
                    System.out.println("budget restored from " + (delayBudget-1) + " to " + delayBudget);
                }

                depthLimitReached = false;
                depth--;
                notifyStateBacktracked();
            }
            prevStateID = vm.getStateId();
            System.out.println("Current stateID: " + vm.getStateId());
            exploredChoiceMap.putIfAbsent(prevStateID, 0);
            if (!(exploredChoiceMap.get(vm.getStateId()) > 0) || delayBudget != 0) { // has budget to forward
                if (forward()) {
                    System.out.println("forwarded to stateID: " + vm.getStateId());
                    depth++;
                    notifyStateAdvanced();
                    if (exploredChoiceMap.get(prevStateID) > 0 ) { // not first choice, decrease budget
                        delayBudget--;
                        System.out.println("budget--, current budget: " + delayBudget);
                    }
                    exploredChoiceMap.putIfAbsent(vm.getStateId(), 0);
                    exploredChoiceMap.put(prevStateID, exploredChoiceMap.get(prevStateID) + 1);
                    System.out.println("Prev State " + prevStateID + " explored choice updated to " + exploredChoiceMap.get(prevStateID));


                    if (currentError != null){
                        notifyPropertyViolated();

                        if (hasPropertyTermination()) {
                            break;
                        }
                        // for search.multiple_errors we go on and treat this as a new state
                        // but hasPropertyTermination() will issue a backtrack request
                    }

                    if (depth >= depthLimit) {
                        depthLimitReached = true;
                        notifySearchConstraintHit("depth limit reached: " + depthLimit);
                        continue;
                    }

                    if (!checkStateSpaceLimit()) {
                        notifySearchConstraintHit("memory limit reached: " + minFreeMemory);
                        // can't go on, we exhausted our memory
                        break;
                    }

                    if (delayBudget == 0) {
                        requestBacktrack();
                    }

                }
            } else { // forward did not execute any instructions
                notifyStateProcessed();
            }
        }

        notifySearchFinished();
    }


    @Override
    public boolean supportsBacktrack () {
        return true;
    }


}
