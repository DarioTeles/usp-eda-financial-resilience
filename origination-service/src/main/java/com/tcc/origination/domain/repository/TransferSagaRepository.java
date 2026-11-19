package com.tcc.origination.domain.repository;

import com.tcc.origination.domain.model.TransferSaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransferSagaRepository extends JpaRepository<TransferSaga, UUID> {
}
