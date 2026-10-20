package com.undec.acreditacion.infrastructure.storage;

import com.undec.acreditacion.domain.exceptions.ArchivoInvalidoException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class InMemoryDocumentStorageAdapter implements DocumentStoragePort {

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Override
    public String uploadFile(byte[] data, String filename, String contentType) {
        if (data == null || data.length == 0) {
            throw new ArchivoInvalidoException("El archivo a subir no puede estar vacío");
        }
        String path;
        if (filename != null && filename.contains("/")) {
            path = filename;
        } else {
            String extension = "";
            if (filename != null && filename.contains(".")) {
                extension = filename.substring(filename.lastIndexOf("."));
            }
            path = UUID.randomUUID() + extension;
        }
        storage.put(path, data);
        return path;
    }

    @Override
    public String getFileUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return "http://localhost:9000/instituciones-logos/" + path;
    }

    @Override
    public String getPresignedDownloadUrl(String path, int duracionMinutos) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return "http://localhost:9000/presigned/" + path + "?exp=" + duracionMinutos;
    }

    @Override
    public byte[] downloadFile(String path) {
        return storage.get(path);
    }

    @Override
    public void deleteFile(String path) {
        storage.remove(path);
    }
}
