package com.undec.acreditacion.domain.ports;

public interface DocumentStoragePort {

    String uploadFile(byte[] data, String filename, String contentType);

    String getFileUrl(String path);

    String getPresignedDownloadUrl(String path, int duracionMinutos);

    byte[] downloadFile(String path);

    void deleteFile(String path);
}
