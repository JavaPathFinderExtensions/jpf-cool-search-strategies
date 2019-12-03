package gov.nasa.jpf.search;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.RestorableVMState;
import gov.nasa.jpf.vm.VM;

import java.util.*;

public class StatelessPCTSearch extends Search {

    private int path_limit;
    private int maxPriority;
    private int maxPrioritySwitchPoints;
    private SortedSet<Integer> priorityChangePoints;
    private LinkedList<Integer> freePriority;
    protected PriorityQueue<DynamicPriorityVMState> childStates;
    protected HashMap<Integer, Integer> priorityMap;
    protected HashMap<Integer, Integer> scheduledStepsMap;
    public static final int DEFAULT_BASE_PRIORITY = 0;
    public static final int DEFAULT_POSITIVE_PRIORITY = 129;


    protected Random rand;

    public StatelessPCTSearch(Config config, VM vm) {
        super(config, vm);
        this.path_limit = config.getInt("search.StatelessPCTSearch.path_limit", 10);
        this.rand = new Random( config.getInt("choice.seed", 42));
        this.maxPriority = 1024;
        this.maxPrioritySwitchPoints = 100;
        this.childStates = new PriorityQueue<>();
        this.priorityMap = new HashMap<>();
        this.scheduledStepsMap = new HashMap<>();
        this.priorityChangePoints = new TreeSet<>();
        this.freePriority = new LinkedList<>();

        for (int i = 0; i < 3; i++) {
            this.priorityChangePoints.add(rand.nextInt(maxPrioritySwitchPoints));
        }
        for (int i = DEFAULT_POSITIVE_PRIORITY; i < maxPriority; i++) {
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
            priorityMap.put(curThreadId, scheduledSteps);
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
                            DynamicPriorityVMState newChildState = new DynamicPriorityVMState(vm, DEFAULT_POSITIVE_PRIORITY);
                            int curThreadId = vm.getCurrentThread().getGlobalId();
                            if (priorityMap.containsKey(curThreadId)) {
                                newChildState.setPriority(priorityMap.get(curThreadId));
                            }
                            else {
                                if (freePriority.isEmpty()) {
                                    for (int i = DEFAULT_POSITIVE_PRIORITY; i < maxPriority; i++) {
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


    private int randIntFromRange(int min, int max) {
        return this.rand.nextInt((max - min) + 1) + min;
    }

    private void reset() {
        depth = 1;
        this.childStates = new PriorityQueue<>();
        this.priorityMap = new HashMap<>();
        this.freePriority = new LinkedList<>();
        this.scheduledStepsMap = new HashMap<>();
        for (int i = DEFAULT_POSITIVE_PRIORITY; i < maxPriority; i++) {
            freePriority.add(i);
        }
        Collections.shuffle(freePriority);
    }


}


