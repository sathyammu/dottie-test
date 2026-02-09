package com.brimmatech.docflow.v2.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TaskDto {
    private Long sequence;
    private int state;
    private String topic;
    private int docExId;
}
