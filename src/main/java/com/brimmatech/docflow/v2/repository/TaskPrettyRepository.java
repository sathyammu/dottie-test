package com.brimmatech.docflow.v2.repository;

import com.brimmatech.docflow.v2.models.TaskPretty;
import com.brimmatech.general.config.TemplateConfig;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TaskPrettyRepository extends JpaRepository<TaskPretty, Integer> {

    Optional<TaskPretty> findBySequence(long sequence);

}
