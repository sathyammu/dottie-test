package com.brimmatech.docflow.common.blob;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlobRequest {
    private String fileName;
    private byte[] documentData;
    private Long size;
}
