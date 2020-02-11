package gov.nasa.jpf.search;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.util.JPFLogger;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.VM;

import java.util.HashMap;

public class DelayBoundedDFS extends Search {
    private int delayBudget;
    private boolean scheduleChoiceOnly;
    private HashMap<Integer, Integer> exploredChoiceMap;  // key: stateID, value: # of choices explored for the state
    static JPFLogger log = JPF.getLogger("gov.nasa.jpf.search.DelayBoundedDFS");
    
    public DelayBoundedDFS(Config conf, VM vm) {
        super(conf, vm);
        this.delayBudget = config.getInt("search.DelayBoundedDFS.delayBudget", 0);
        this.scheduleChoiceOnly = config.getBoolean("search.DelayBoundedDFS.scheduleCostOnly", false);
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
                log.info("backtracked to stateID: " + vm.getStateId());
                if (!haveFreeForward(vm.getStateId() ,1)) {
                    // since we backtracked, we get back one delay budget if it cost us to exploring prev state
                    delayBudget++;
                    log.info("budget restored from " + (delayBudget-1) + " to " + delayBudget);
                }
                depthLimitReached = false;
                depth--;
                notifyStateBacktracked();
            }
            
            prevStateID = vm.getStateId();
            exploredChoiceMap.putIfAbsent(prevStateID, 0);
            // allow vm to forward only if we are doing a free forward or have budget
            if (haveFreeForward(vm.getStateId(), 0) || delayBudget > 0) {
                if (forward()) {
                    log.info("forwarded to stateID: " + vm.getStateId());
                    depth++;
                    notifyStateAdvanced();
                    if (!haveFreeForward(prevStateID, 0)) {
                        delayBudget--;
                        log.info("budget--, current budget: " + delayBudget);
                    }
                    exploredChoiceMap.putIfAbsent(vm.getStateId(), 0);
                    exploredChoiceMap.put(prevStateID, exploredChoiceMap.get(prevStateID) + 1);
                    log.info("Prev State " + prevStateID + " explored choice count updated to "
                            + exploredChoiceMap.get(prevStateID));
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
                } else {
                    // forward did not execute any instructions
                    requestBacktrack();
                    notifyStateProcessed();
                }
            } else {
                requestBacktrack();
            }
        }
        notifySearchFinished();
    }


    @Override
    public boolean supportsBacktrack () {
        return true;
    }

    private boolean haveFreeForward(int stateId, int maxExploredAllowed) {
        ChoiceGenerator<?> curCg = vm.getChoiceGenerator();
        return (scheduleChoiceOnly && curCg!= null && !vm.getChoiceGenerator().isSchedulingPoint())
                || exploredChoiceMap.get(stateId) <= maxExploredAllowed;
    }


}
