package com.brimmatech.saas;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PipelinePaginationRequest {
    private Term filter;
    private Set<String> fields;
    private List<SortItem> sortOrder;
    private String orgType;
    private String loanOwnership;
    private List<String> loanFolders;

}

@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings({"PMD.ShortClassName"})
class SortItem implements Serializable {
    private String canonicalName;
    private String order;
    private String fieldId;
}