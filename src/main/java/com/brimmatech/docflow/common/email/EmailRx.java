package com.brimmatech.docflow.common.email;

import com.brimmatech.docflow.v2.task.globalapi.actions.ActionTopicDto;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data public class EmailRx {
    Map<EmailTopic, Rx> topicRecipients;
    Map<String, ActionTopicDto> actionItems;

    @RequiredArgsConstructor
    @Getter
    public enum EmailTopic {
        PACKAGE_SPLIT(true),
        EXCEPTION_NOTIFICATION(true),
        HOOK_DELIVERY_FAILURE(true),
        USNAT_FEE_UPDATE(true),
        USNAT_GENIE_FEE_INVALID(false),
        USNAT_INVALID_PASSWORD(true),
        USNAT_PROGRESS_NOTIFICATION(true),
        USNAT_SITE_NOT_REACHABLE(true),
        BRIMMA_GLOBAL_SUPPORT(false),
        OAK_TREE_EXTRACTION(true),
        MODEL_TRAINERS(true),
        PACKAGE_RESULT_SUMMARY(true),
        PURCHASE_ADVICE(true),
        INTENT_TO_PROCEED(true);

        private final boolean shouldApplyGlobals;
    }

    public record Rx(List<String> to,
            List<String> cc,
            List<String> bcc){

        public void populateRx(EmailRequest request) {
            request.setToList(to);
            request.setCcList(cc);
            request.setBccList(bcc);
        }

        public Rx merge(Rx other) {
            List<String> mergedTo = new ArrayList<>(this.to != null ? this.to : List.of());
            List<String> mergedCc = new ArrayList<>(this.cc != null ? this.cc : List.of());
            List<String> mergedBcc = new ArrayList<>(this.bcc != null ? this.bcc : List.of());

            if (other != null) {
                if (other.to != null) mergedTo.addAll(other.to);
                if (other.cc != null) mergedCc.addAll(other.cc);
                if (other.bcc != null) mergedBcc.addAll(other.bcc);
            }

            return new Rx(List.copyOf(mergedTo), List.copyOf(mergedCc), List.copyOf(mergedBcc));
        }

    };

    public EmailRx mergeWith(EmailRx other) {
        Map<EmailTopic, Rx> mergedRecipients = new HashMap<>();

        for (EmailTopic topic : EmailTopic.values()) {
            Rx thisRx = this.topicRecipients.get(topic);
            Rx otherRx = other.topicRecipients.get(topic);

            List<String> mergedTo = new ArrayList<>();
            List<String> mergedCc = new ArrayList<>();
            List<String> mergedBcc = new ArrayList<>();

            if (thisRx != null) {
                mergedTo.addAll(thisRx.to());
                mergedCc.addAll(thisRx.cc());
                mergedBcc.addAll(thisRx.bcc());
            }

            if (otherRx != null) {
                mergedTo.addAll(otherRx.to());
                mergedCc.addAll(otherRx.cc());
                mergedBcc.addAll(otherRx.bcc());
            }

            mergedRecipients.put(topic, new Rx(mergedTo, mergedCc, mergedBcc));
        }

        EmailRx mergedEmailRx = new EmailRx();
        mergedEmailRx.topicRecipients = mergedRecipients;
        return mergedEmailRx;
    }
}
