package org.example.springboot.controller;

import org.apache.tika.exception.TikaException;
import org.example.springboot.agent.parser.dto.ParseResult;
import org.example.springboot.agent.parser.service.TikaParseService;
import org.example.springboot.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;

@RestController
@RequestMapping("doc")
public class TikaParseController {
    @Autowired
    private TikaParseService tikaParseService;

    /**
     * 解析上传的文档，返回文本和元数据
     *
     * POST /api/document/parse
     * Content-Type: multipart/form-data
     */
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ParseResult> parseDocument(@RequestParam("file") MultipartFile file) throws TikaException, IOException, SAXException {
        ParseResult result = tikaParseService.parseFile(file);

        if (result.isSuccess()) {
            return Result.success(result);
        } else {
            return Result.error();
        }
    }
}
