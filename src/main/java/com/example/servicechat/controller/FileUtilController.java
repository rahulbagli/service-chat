package com.example.servicechat.controller;

import com.example.servicechat.service.FileUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@CrossOrigin(
        origins = "http://localhost:5173",
        allowCredentials = "true"
)
@Controller
public class FileUtilController {

    @Autowired
    private FileUtilService fileUtilService;

    @GetMapping("/download-file")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam("fileName") String fileName,
            @RequestParam("queryIntent") String queryIntent
    ) {
        try {

            return new ResponseEntity<>(fileUtilService.downloadFile(fileName, queryIntent), HttpStatus.OK);
//            Resource resource = fileUtilService.downloadFile(fileName, queryIntent);
//            return ResponseEntity.ok()
//                    .header(HttpHeaders.CONTENT_DISPOSITION,
//                            "attachment; filename=\"" + resource.getFilename() + "\"")
//                    .header(HttpHeaders.CONTENT_TYPE,
//                            MediaType.APPLICATION_OCTET_STREAM_VALUE)
//                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
