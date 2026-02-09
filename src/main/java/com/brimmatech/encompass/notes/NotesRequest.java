package com.brimmatech.encompass.notes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotesRequest {
    private String id;
    private String value;
    private boolean lock;
    private String latestNote;
    private String userInitial;
}