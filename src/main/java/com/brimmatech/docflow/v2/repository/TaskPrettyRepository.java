package com.brimmatech.docflow.v2.repository;

import com.brimmatech.docflow.v2.models.TaskPretty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskPrettyRepository extends JpaRepository<TaskPretty, Integer> {

    Optional<TaskPretty> findBySequence(long sequence);

    List<TaskPretty> findBySequenceInOrderByCreatedAsc(List<Integer> sequence);

}
