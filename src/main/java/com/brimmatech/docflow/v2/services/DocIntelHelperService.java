package com.brimmatech.docflow.v2.services;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service @Slf4j @RequiredArgsConstructor public class DocIntelHelperService {

    public String generateSanitizedAttachmentName(String attachmentName) {
        String sanitizedFileName = attachmentName.replaceAll("[^a-zA-Z0-9.]+", "_");

        int lastDotIndex = sanitizedFileName.lastIndexOf('.');
        String baseName = sanitizedFileName;
        String extension = "";

        if (lastDotIndex != -1) {
            baseName = sanitizedFileName.substring(0, lastDotIndex);
            extension = sanitizedFileName.substring(lastDotIndex); // includes the dot
        }

        String uuid = NanoIdUtils.randomNanoId();
        return baseName + "_" + uuid + extension;
    }

}
