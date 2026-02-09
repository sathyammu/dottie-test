package com.brimmatech.encompass.documentcreator;

import java.util.List;
import java.util.Optional;

public interface DocumentProcessor{
    <T> List<T> fetchAllDocumentDetails(String loanGuid, String accessToken);

    <T> List<T> createDocument(String loanGuid, String accessToken, Object request);

    Optional<String> getDocumentId(String loanGuid, String accessToken, String folderName);
    Optional<String> getOrCreateReturningDocumentId(String loanGuid, String accessToken, String folderName);

    <T> List<T> addComments(String loanGuid, String documentGuid, String accessToken, Object comment);
}
