package com.brimmatech.encompass.documentcreator.encompassdocuments;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DocumentRequest {
    private String title;
}
