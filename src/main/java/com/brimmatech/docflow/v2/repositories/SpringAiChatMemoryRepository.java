package com.brimmatech.docflow.v2.repositories;

import com.brimmatech.docflow.v2.models.SpringAiChatMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.*;
import java.util.List;

public interface SpringAiChatMemoryRepository extends JpaRepository<SpringAiChatMemory, Integer> {

    List<SpringAiChatMemory> findByConversationId(String conversationId);
}
