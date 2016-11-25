package integratedtoolkit.types;

import integratedtoolkit.api.COMPSsRuntime.DataDirection;
import integratedtoolkit.api.COMPSsRuntime.DataType;
import integratedtoolkit.types.annotations.Constants;
import integratedtoolkit.types.implementations.Implementation.TaskType;
import integratedtoolkit.types.parameter.Parameter;
import integratedtoolkit.util.CoreManager;

import java.io.Serializable;


public class TaskDescription implements Serializable {

    /**
     * Serializable objects Version UID are 1L in all Runtime
     */
    private static final long serialVersionUID = 1L;

    private final TaskType type;
    private final String methodName;
    private final Integer coreId;
    
    private final boolean priority;
    private final int numNodes;
    private final boolean mustReplicate;
    private final boolean mustDistribute;

    private final Parameter[] parameters;
    private final boolean hasTarget;
    private final boolean hasReturn;



    public TaskDescription(String methodClass, String methodName, boolean isPrioritary, int numNodes, boolean isReplicated, 
            boolean isDistributed, boolean hasTarget, Parameter[] parameters) {
        
        this.type = TaskType.METHOD;
        this.methodName = methodName;
        
        this.priority = isPrioritary;
        this.numNodes = numNodes;
        this.mustReplicate = isReplicated;
        this.mustDistribute = isDistributed;
        
        this.hasTarget = hasTarget;
        this.parameters = parameters;
        if (parameters.length == 0) {
            this.hasReturn = false;
        } else {
            Parameter lastParam = parameters[parameters.length - 1];
            DataType type = lastParam.getType();
            this.hasReturn = (lastParam.getDirection() == DataDirection.OUT 
                                && (type == DataType.OBJECT_T || type == DataType.PSCO_T || type == DataType.EXTERNAL_PSCO_T));
        }
        
        this.coreId = CoreManager.getCoreId(methodClass, methodName, hasTarget, hasReturn, parameters);
    }

    public TaskDescription(String namespace, String service, String port, String operation, boolean isPrioritary, boolean hasTarget,
            Parameter[] parameters) {
        
        this.type = TaskType.SERVICE;
        this.methodName = operation;
        
        this.priority = isPrioritary;
        this.numNodes = Constants.SINGLE_NODE;
        this.mustReplicate = !Constants.REPLICATED_TASK;
        this.mustDistribute = !Constants.DISTRIBUTED_TASK;
        
        this.hasTarget = hasTarget;
        this.parameters = parameters;
        if (parameters.length == 0) {
            this.hasReturn = false;
        } else {
            Parameter lastParam = parameters[parameters.length - 1];
            DataType type = lastParam.getType();
            this.hasReturn = (lastParam.getDirection() == DataDirection.OUT 
                                && (type == DataType.OBJECT_T || type == DataType.PSCO_T || type == DataType.EXTERNAL_PSCO_T));
        }
        
        this.coreId = CoreManager.getCoreId(namespace, service, port, operation, hasTarget, hasReturn, parameters);
    }

    public Integer getId() {
        return coreId;
    }

    public String getName() {
        return methodName;
    }

    public boolean hasPriority() {
        return priority;
    }
    
    public int getNumNodes() {
        return numNodes;
    }
    
    public boolean isSingleNode() {
        return numNodes == Constants.SINGLE_NODE;
    }
    
    public boolean isReplicated() {
        return mustReplicate;
    }
    
    public boolean isDistributed() {
        return mustDistribute;
    }
    
    public Parameter[] getParameters() {
        return parameters;
    }

    public boolean hasTargetObject() {
        return hasTarget;
    }

    public boolean hasReturnValue() {
        return hasReturn;
    }

    public TaskType getType() {
        return this.type;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("[Core id: ").append(this.coreId).append("]");
        
        buffer.append(", [Priority: ").append(this.priority).append("]");
        buffer.append(", [NumNodes: ").append(this.numNodes).append("]");
        buffer.append(", [MustReplicate: ").append(this.mustReplicate).append("]");
        buffer.append(", [MustDistribute: ").append(this.mustDistribute).append("]");
        
        buffer.append(", [").append(getName()).append("(");
        int numParams = this.parameters.length;
        if (this.hasTarget) {
            numParams--;
        }
        if (this.hasReturn) {
            numParams--;
        }
        if (numParams > 0) {
            buffer.append(this.parameters[0].getType());
            for (int i = 1; i < numParams; i++) {
                buffer.append(", ").append(this.parameters[i].getType());
            }
        }
        buffer.append(")]");
        
        return buffer.toString();
    }

}
