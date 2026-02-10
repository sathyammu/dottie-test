package com.brimmatech.princeton.dottie;

import com.brimmatech.docflow.classification.assistants.SystemPromptProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static com.brimmatech.docflow.enums.SettingsCategory.SYS_PROMPTS_PRINCETON_SUMMARY;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/llm")
public class LoanSummaryController {

    private final ChatClient.Builder llmClientBuilder;
    private final SystemPromptProvider systemPromptProvider;

    @PostMapping("/summary")
    public ResponseEntity<Flux<String>> generateLoanSummary(
            @RequestBody LoanSummaryRequest request) {

        ChatClient client = llmClientBuilder.clone().build();

        String systemPrompt = systemPromptProvider
                .getSystemMessage(-1, SYS_PROMPTS_PRINCETON_SUMMARY)
                .getText();

        Flux<String> response =
                client.prompt()
                        .system(systemPrompt)
                        .user(request.loanObject().toString())
                        .stream()
                        .content();

        return ResponseEntity.ok(response);
    }

    public record LoanSummaryRequest(Object loanObject) {
    }
}

