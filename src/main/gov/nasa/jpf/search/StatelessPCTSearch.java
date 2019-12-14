package gov.nasa.jpf.search;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.RestorableVMState;
import gov.nasa.jpf.vm.VM;

import java.util.*;

public class StatelessPCTSearch extends Search {

    private int path_limit;
    private int n;
    private int maxPrioritySwitchPoints;  // k
    private int numPrioritySwitchPoints;  // d - 1
    private SortedSet<Integer> priorityChangePoints;
    private LinkedList<Integer> freePriority;
    protected PriorityQueue<DynamicPriorityVMState> childStates;
    protected HashMap<Integer, Integer> priorityMap;
    protected HashMap<Integer, Integer> scheduledStepsMap;


    protected Random rand;

    public StatelessPCTSearch(Config config, VM vm) {
        super(config, vm);
        this.path_limit = config.getInt("search.StatelessPCTSearch.path_limit", 100);
        this.rand = new Random( config.getInt("choice.seed", 42));
        this.n = config.getInt("search.StatelessPCTSearch.n", 5);
        this.numPrioritySwitchPoints = config.getInt("search.StatelessPCTSearch.numPrioritySwitchPoints", 1);
        this.maxPrioritySwitchPoints = config.getInt("search.StatelessPCTSearch.maxPrioritySwitchPoints", 10);
        this.childStates = new PriorityQueue<>();
        this.priorityMap = new HashMap<>();
        this.scheduledStepsMap = new HashMap<>();
        this.priorityChangePoints = new TreeSet<>();
        this.freePriority = new LinkedList<>();

        for (int i = 0; i < maxPrioritySwitchPoints; i++) {
            this.priorityChangePoints.add(rand.nextInt(maxPrioritySwitchPoints));
        }
        for (int i = numPrioritySwitchPoints + 1; i < numPrioritySwitchPoints + 1 + n; i++) { // d ~ d + n
            freePriority.add(i);
        }
        Collections.shuffle(freePriority);
    }

    @Override
    public void search () {
        depth = 0;
        int paths = 0;
        depth++;

        if (hasPropertyTermination()) {
            return;
        }
        //vm.forward();
        RestorableVMState init_state = vm.getRestorableState();
        notifySearchStarted();
        while (!done) {
            if (!isEndState() && (depth < depthLimit) && PCTForward()) {
                notifyStateAdvanced();

                if (currentError != null){
                    notifyPropertyViolated();

                    if (hasPropertyTermination()) {
                        return;
                    }
                }
                depth++;
            } else { // no next state or reached depth limit
                // <2do> we could check for more things here. If the last insn wasn't
                // the main return, or a System.exit() call, we could flag a JPFException
                if (depth >= depthLimit) {
                    notifySearchConstraintHit("depth limit reached: " + depthLimit);
                }
                checkPropertyViolation();
                if (hasPropertyTermination()) {
                    return;
                }
                done = (paths >= path_limit);
                paths++;
                System.out.println("=========== paths = " + paths + "===============");
                reset();
                vm.restoreState(init_state);
                vm.resetNextCG();
            }
            System.out.println("depth: " + depth);
        }
        notifySearchFinished();
    }

    private boolean PCTForward() {



        if (!generateChildren() ||childStates.size() == 0) {
            return false;
        }
        DynamicPriorityVMState dState = childStates.poll();
        restoreState(dState);
        int curThreadId = vm.getCurrentThread().getGlobalId();
        int scheduledSteps;
        if (scheduledStepsMap.containsKey(curThreadId)) {
            scheduledSteps = scheduledStepsMap.get(curThreadId) + 1;
        }
        else {
            scheduledSteps = 1;
        }
        scheduledStepsMap.put(curThreadId, scheduledSteps);
        if (priorityChangePoints.contains(scheduledSteps)) {
            priorityMap.put(curThreadId, priorityChangePoints.headSet(scheduledSteps).size() - 1); // index
        }
        return true;
    }


    private boolean generateChildren() {

        childStates = new PriorityQueue<>();

        while (!done) {

            if (!forward()) {
                notifyStateProcessed();
                return true;
            }

            depth++;
            notifyStateAdvanced();
            if (currentError != null){
                notifyPropertyViolated();
                if (hasPropertyTermination()) {
                    return false;
                }
            } else {

                if (!isEndState() && !isIgnoredState()) {

                    if (depth >= depthLimit) {
                        // we can't do this before we actually generated the VM child state
                        // since we don't want to report DEPTH_CONSTRAINTs for parents
                        // that have only visited or end state children
                        notifySearchConstraintHit("depth limit reached: " + depthLimit);

                    } else {
                        RestorableVMState newState = vm.getRestorableState();
                        if (newState != null) {
                            DynamicPriorityVMState newChildState = new DynamicPriorityVMState(vm, numPrioritySwitchPoints + 1);
                            int curThreadId = vm.getCurrentThread().getGlobalId();
                            if (priorityMap.containsKey(curThreadId)) {
                                newChildState.setPriority(priorityMap.get(curThreadId));
                            }
                            else {
                                if (freePriority.isEmpty()) {
                                    for (int i = numPrioritySwitchPoints + 1; i < numPrioritySwitchPoints + 1 + n; i++) { // d ~ d + n
                                        freePriority.add(i);
                                    }
                                    Collections.shuffle(freePriority);
                                }
                                int nextFreePriority = freePriority.removeFirst();
                                priorityMap.put(curThreadId, nextFreePriority);
                                newChildState.setPriority(nextFreePriority);
                            }

                            childStates.add(newChildState);
                        }
                    }
                } else {
                    // end state or ignored transition
                }
            }
            backtrackToParent();
        }
        return false;
    }

    private void backtrackToParent() {
        backtrack();
        depth--;
        notifyStateBacktracked();
    }

    private void restoreState (DynamicPriorityVMState dState) {
        vm.restoreState(dState.getVMState());
        depth = vm.getPathLength();
        notifyStateRestored();
    }

    private void reset() {
        depth = 1;
        this.childStates = new PriorityQueue<>();
        this.priorityMap = new HashMap<>();
        this.freePriority = new LinkedList<>();
        this.scheduledStepsMap = new HashMap<>();
        for (int i = numPrioritySwitchPoints + 1; i < numPrioritySwitchPoints + 1 + n; i++) { // d ~ d + n
            freePriority.add(i);
        }
        Collections.shuffle(freePriority);
    }


}


