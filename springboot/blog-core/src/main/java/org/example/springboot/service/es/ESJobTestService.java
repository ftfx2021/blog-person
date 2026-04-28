package org.example.springboot.service.es;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.entity.es.JobTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.relational.core.mapping.event.BeforeSaveCallback;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ESJobTestService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public String save(JobTest jobTest) {
        JobTest savedEntity = elasticsearchOperations.save(jobTest);
        log.info("savedEntity.indexName={}", savedEntity.getIndexName());

        return savedEntity.getId();
    }
    public JobTest get(String id) {
        return elasticsearchOperations.get(id, JobTest.class);
    }

}
