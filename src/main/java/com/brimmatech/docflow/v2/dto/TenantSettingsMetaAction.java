package com.brimmatech.docflow.v2.dto;

import com.brimmatech.general.config.RoutingTypes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter public class TenantSettingsMetaAction {
    private String type;
    private RuleActionArgs actionArgs;
    private Boolean manualTrigger;

    public RoutingTypes getRoute() {
        return actionArgs.routeName;
    }

    public enum ChecksumBehaviour {
        use_cached_results, run_always
    }

    public record DynamicReplacementsForTaskCreation(int stepIndex, String path, JsonNode value) {
    }

    public record RuleActionArgs(RoutingTypes routeName,
                                 ChecksumBehaviour checksumBehaviour,
                                 List<DynamicReplacementsForTaskCreation> dynamicReplacementsForTaskCreation,
                                 String signedDisclosureDocumentName,
                                 String signedDisclosureAttachmentName,
                                 List <Substitutions> substitutions) {
    }

    public record Substitutions(String targetJsonPath, ObjectNode value, SubstitutionType changeType) {
    }

    public enum SubstitutionType{
        ADD,
        EDIT,
        DELETE
    }
    public record PathResult(JsonNode parent, String key) {}

}
