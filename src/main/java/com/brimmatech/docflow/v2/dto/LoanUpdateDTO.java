package com.brimmatech.docflow.v2.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoanUpdateDTO {
    private String id;
    private String value;
    private boolean lock;

}
