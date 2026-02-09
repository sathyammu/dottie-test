package com.brimmatech.encompass.loanreader.fieldReader;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FieldReaderResponse {
    private String fieldId;
    private Object value;
}
