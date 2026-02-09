package com.brimmatech.docflow.v2.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "spring_ai_chat_memory")
public class SpringAiChatMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "conversation_id")
    private String conversationId;

    private String content;

    private String type;

    @CreatedDate
    private LocalDateTime timestamp;
}
