package com.brimmatech.encompass.documentcreator.encompassdocuments;

import com.brimmatech.encompass.documentcreator.DocumentProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service("encompass-document-processor")
@Slf4j
public class EncompassDocumentProcessor implements DocumentProcessor {

    @Autowired
    ObjectMapper objectMapper;
    @Value("${api.encompass.host}")
    private String encompassBaseUrl;
    @Value("${api.encompass.endpoint.fetch-all-document-uri}")
    private String fetchAllDocumentsUri;
    @Value("${api.encompass.endpoint.create-document-uri}")
    private String createNewDocumentUri;
    @Value("${api.encompass.endpoint.get-document-uri}")
    private String getDocumentUri;
    @Value("${api.encompass.endpoint.add-comment-uri}")
    private String addCommentUri;
    private WebClient webClient;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {

        final int size = 16 * 1024 * 1024;

        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();

        webClient = WebClient.builder().baseUrl(encompassBaseUrl).exchangeStrategies(strategies).build();
    }

    @Override
    public List<Document> fetchAllDocumentDetails(String loanGuid, String accessToken) {

        List<Document> documents = null;
        try {

            documents = webClient.get()
                    .uri(fetchAllDocumentsUri, loanGuid)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Document>>() {
                    })
                    .block();

            log.info("Fetched all document details for a loan :{} ", loanGuid);

        } catch (Exception e) {
            log.error(":{}", e.getMessage());
            // throw exception
        }

        return documents;
    }

    @Override
    public List<Document> createDocument(String loanGuid, String accessToken, Object documentRequest) {

        DocumentRequest request = objectMapper.convertValue(documentRequest, DocumentRequest.class);

        List<DocumentRequest> documentRequests = Collections.singletonList(request);

        List<Document> documentDetail = null;

        documentDetail = webClient.patch()
                .uri(createNewDocumentUri, loanGuid)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .bodyValue(documentRequests)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Document>>() {
                })
                .block();

        log.info("Create a new document for a loan :{} ", loanGuid);
        return documentDetail;
    }

    @Override
    public List<CommentDetails> addComments(String loanGuid, String documentGuid, String accessToken, Object comment) {

        Comment request = objectMapper.convertValue(comment, Comment.class);

        List<CommentDetails> commentResponse = null;
        try {

            commentResponse = webClient.patch()
                    .uri(addCommentUri, loanGuid, documentGuid)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .bodyValue(Collections.singletonList(request))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<CommentDetails>>() {
                    })
                    .block();

            log.info("Successfully Added comment for a loan :{} ", loanGuid);

        } catch (Exception e) {
            log.error(":{}", e.getMessage());
            // throw exception
        }

        return commentResponse;
    }

    @Override public Optional<String> getDocumentId(String loanGuid, String accessToken, String folderName) {
        try {
            Optional<Document>
                    first =
                    fetchAllDocumentDetails(loanGuid, accessToken).stream()
                            .filter(v -> v.getTitle().equalsIgnoreCase(folderName))
                            .findFirst();

            if (first.isEmpty()) {
                return Optional.empty();
            }

            val documentDetail = webClient.get()
                    .uri(getDocumentUri, loanGuid, first.get().getId())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ObjectNode>() {
                    })
                    .block();
            if (Objects.isNull(documentDetail)) {
                return Optional.empty();
            }
            JsonNode at = documentDetail.at("/id");
            return at.isMissingNode() ? Optional.empty() : Optional.ofNullable(at.asText());

        } catch (Exception e) {
            log.error(":{}", e.getMessage());
            // throw exception
        }
        return Optional.empty();
    }

    public Optional<String> getOrCreateReturningDocumentId(String loanGuid, String accessToken, String folderName) {
        return this.getDocumentId(loanGuid, accessToken, folderName).or(() -> {
            List<Document> document = this.createDocument(loanGuid, accessToken,
                    DocumentRequest.builder().title(folderName).build());

            return document.isEmpty() ? Optional.empty() : Optional.ofNullable(document.getFirst().getId());
        });
    }
}
