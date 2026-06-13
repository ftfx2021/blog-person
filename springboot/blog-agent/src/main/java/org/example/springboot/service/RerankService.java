package org.example.springboot.service;

import java.util.List;

public interface RerankService {
    List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN);}
