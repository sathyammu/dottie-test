package com.brimmatech.docflow.superadmin;

import com.brimmatech.docflow.superadmin.dto.PreferenceRequest;
import com.brimmatech.docflow.superadmin.dto.PreferenceResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserFilterPreferenceController {

    private final UserFilterPreferenceService service;

    @PostMapping("/custom-filter")
    public PreferenceResponse saveFilter(@RequestBody PreferenceRequest request) throws Exception {
        return service.saveOrPatchPreference(request);
    }

    @GetMapping("/{loEmail}/custom-filter")
    public PreferenceResponse getFilter(@PathVariable String loEmail) {
        return service.getPreferenceByEmail(loEmail);
    }


    @PostMapping("/update/comments")
    public String saveLoanCondition(
            @RequestParam String loanId,
            @RequestParam String conditionId,
            @RequestParam(required = false) String conditionType,
            @RequestParam() String userId,
            @RequestBody JsonNode comments
    ) {
        String response = service.saveLoanCondition(loanId, conditionId, conditionType,userId, comments);
        return response;
    }

}

