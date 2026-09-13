package com.chitram.shared.service;

public interface StorageService {

    String upload(byte[] content, String storagePath, String contentType);

    void delete(String storagePath);
}
