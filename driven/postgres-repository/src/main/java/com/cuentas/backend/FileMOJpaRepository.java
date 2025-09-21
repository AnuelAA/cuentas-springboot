package com.cuentas.backend;

import com.cuentas.backend.models.FileMO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface FileMOJpaRepository extends JpaRepository<FileMO, Integer> {
}
