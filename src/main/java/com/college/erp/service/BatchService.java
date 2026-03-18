package com.college.erp.service;

import com.college.erp.model.Batch;
import com.college.erp.model.Department;
import com.college.erp.repository.BatchRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BatchService {
    private final BatchRepository batchRepository;

    public BatchService(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public List<Batch> getAllBatches() {
        return batchRepository.findAll();
    }

    public List<Batch> getBatchesByDepartment(Department department) {
        return batchRepository.findByDepartment(department);
    }

    public Batch createBatch(Batch batch) {
        return batchRepository.save(batch);
    }

    public void deleteBatch(Long id) {
        batchRepository.deleteById(id);
    }
}
