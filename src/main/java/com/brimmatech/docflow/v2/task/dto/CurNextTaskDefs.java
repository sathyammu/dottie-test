package com.brimmatech.docflow.v2.task.dto;

import com.brimmatech.general.types.Tuple;
import lombok.Getter;

import java.util.Optional;

@Getter
public class CurNextTaskDefs extends Tuple<Optional<TaskStepDef>, Optional<TaskStepDef>> {

    String routeName;
    int nextIndex;
    public CurNextTaskDefs(Optional<TaskStepDef> left, Optional<TaskStepDef> right, String routeName, int size) {
        super(left, right);
        this.routeName = routeName;
        nextIndex = size;
    }
    
}
