package com.brimmatech.docflow.common.blob;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
@Slf4j
public class BlobProcessorController {

    private BlobProcessor blobProcessor;

    @GetMapping("/blob/saas-token")
    public String getBlobSaasToken(@RequestParam String blobName){
        log.info("Generating blob saas token for :{}" ,blobName);
        return blobProcessor.generateBlobSasToken(blobName, null).url();
    }
}
