package com.cuentas.backend;

import com.cuentas.backend.models.FileMO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileNativeRepository extends JpaRepository<FileMO, Integer> {

    @Query(value = "SELECT * FROM records WHERE file_id = ?1", nativeQuery = true)
    List<Object[]> findRecordsByFileId(Long fileId);

    @Query(value = "SELECT * FROM categories WHERE id = ?1", nativeQuery = true)
    Object[] findCategoryById(Long categoryId);

    @Query(value = "INSERT INTO records (file_id, description, amount, created_at) " +
            "VALUES (?1, ?2, ?3, NOW()) RETURNING id", nativeQuery = true)
    Long insertRecord(Long fileId, String description, Double amount);
}
