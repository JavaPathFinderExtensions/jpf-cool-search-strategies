package gov.nasa.jpf.search;

import gov.nasa.jpf.vm.RestorableVMState;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.VM;

import java.util.Random;

public class DynamicPriorityVMState implements  Comparable<DynamicPriorityVMState>{
    protected RestorableVMState vmState;
    protected int stateId;
    protected int priority;
    protected ThreadInfo threadInfo;

    public DynamicPriorityVMState (VM vm, int priority) {
        stateId = vm.getStateId();
        vmState = vm.getRestorableState();
        threadInfo = vm.getCurrentThread();
        this.priority = priority;
    }

    public RestorableVMState getVMState () {
        return vmState;
    }

    public int getStateId() {
        return stateId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(DynamicPriorityVMState o)
    {
        if (this.getPriority() < o.getPriority()) {
            return -1;
        }
        if (this.getPriority() > o.getPriority()) {
            return 1;
        }
        int rand = new Random().nextInt(2);
        if (rand == 0) {
            return 1;
        }
        else {
            return -1;
        }
    }

    @Override
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof DynamicPriorityVMState)) {
            return false;
        }

        // typecast o to Complex so that we can compare data members
        DynamicPriorityVMState c = (DynamicPriorityVMState) o;

        // Compare the data members and return accordingly
        return this.threadInfo.equals(c.threadInfo);
    }

}
