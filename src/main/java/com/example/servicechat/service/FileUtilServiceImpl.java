package com.example.servicechat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.invoke.MethodHandles;

import static com.example.servicechat.constants.AppConstants.*;

@Service
public class FileUtilServiceImpl implements FileUtilService {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Override
    public Resource downloadFile(String fileName, String queryIntent) {
        String outputDirectory = getOutputDirectory(queryIntent);
        log.info("Output directory: {}", outputDirectory);

        String fullPath = STORE_DRIVE + outputDirectory + "/" + fileName;
        File file = new File(fullPath);
        log.info("File : {}", file);
        try {
            if (!file.exists()) {
                throw new FileNotFoundException("File not found at path: " + fullPath);
            }
            return new InputStreamResource(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private String getOutputDirectory(String queryIntent) {
        return switch (queryIntent) {
            case "get_postman" -> POSTMAN;
            case "get_log" -> APPLICATION_LOG;
            default -> null;
        };
    }
}
