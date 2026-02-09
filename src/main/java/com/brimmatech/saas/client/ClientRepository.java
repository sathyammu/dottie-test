package com.brimmatech.saas.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    Client findByEmailContainingIgnoreCase(String email);
    Client findByClientNameIgnoreCase(String tenantName);
}
