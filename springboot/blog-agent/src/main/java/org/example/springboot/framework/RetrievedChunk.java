package org.example.springboot.framework;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 召回片段
 */
@Data
@AllArgsConstructor
public class RetrievedChunk {

    /**
     * 召回id
     */
    int id;

    /**
     * 命中片段
     */
    String text;

    /**
     * 命中得分
     */
    Float score;

}
