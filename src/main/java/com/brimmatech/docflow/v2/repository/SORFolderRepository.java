package com.brimmatech.docflow.v2.repository;

import com.brimmatech.docflow.v2.models.SORFoldersEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SORFolderRepository extends JpaRepository<SORFoldersEntity,Integer> {

    List<SORFoldersEntity> findByTenant_IdAndIsListenedByValliaDocFlow(Long tenantId, Boolean isListenedByValliaDocFlow);
}
