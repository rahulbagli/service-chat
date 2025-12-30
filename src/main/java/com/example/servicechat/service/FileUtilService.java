package com.example.servicechat.service;

import org.springframework.core.io.Resource;

public interface FileUtilService {

    Resource downloadFile(String fileName, String queryIntent);
}
