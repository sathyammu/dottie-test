package com.brimmatech.docflow.v2.services;

import com.brimmatech.docflow.common.blob.BlobProcessor;
import com.brimmatech.docflow.common.blob.BlobRequest;
import com.brimmatech.general.types.Tuple;
import lombok.Builder;
import lombok.With;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class AzureBlobService {

    @Autowired
    BlobProcessor blobProcessor;

    public Tuple<BlobRequest, byte[]> uploadAttachmentToBlobStorage(byte[] file, String sanitizedFileName
    ) {
        MessageDigest md = null;
        byte[] md5Bytes;
        try {
            md = MessageDigest.getInstance("MD5");
            md5Bytes = md.digest(file);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        BlobRequest blobRequest = new BlobRequest();
        blobRequest.setDocumentData(file);
        blobRequest.setFileName(sanitizedFileName);

        blobProcessor.uploadAttachmentToStorage(blobRequest, md5Bytes, null);
        return new Tuple<>(blobRequest, md5Bytes);

    }


    @Builder
    public record BlobPdDocumentInput(Path persistedLocation, String fileName, @With String etag) {
    }

}
