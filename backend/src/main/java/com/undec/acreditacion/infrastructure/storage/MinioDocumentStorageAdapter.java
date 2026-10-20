package com.undec.acreditacion.infrastructure.storage;

import com.undec.acreditacion.domain.exceptions.ArchivoInvalidoException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Profile("!test")
public class MinioDocumentStorageAdapter implements DocumentStoragePort {

    private static final Logger log = LoggerFactory.getLogger(MinioDocumentStorageAdapter.class);

    private final MinioClient minioClient;
    private final String bucketName;
    private final String endpoint;

    public MinioDocumentStorageAdapter(MinioClient minioClient,
                                     @Value("${minio.bucket:instituciones-logos}") String bucketName,
                                     @Value("${minio.endpoint:http://localhost:9000}") String endpoint) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.endpoint = endpoint;
    }

    @Override
    public String uploadFile(byte[] data, String filename, String contentType) {
        if (data == null || data.length == 0) {
            throw new ArchivoInvalidoException("El archivo a subir no puede estar vacío");
        }

        try {
            ensureBucketExists();

            String objectName;
            if (filename != null && filename.contains("/")) {
                objectName = filename;
            } else {
                String extension = "";
                if (filename != null && filename.contains(".")) {
                    extension = filename.substring(filename.lastIndexOf("."));
                }
                objectName = UUID.randomUUID() + extension;
            }

            try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(bais, data.length, -1)
                                .contentType(contentType != null ? contentType : "application/octet-stream")
                                .build()
                );
            }

            log.info("Archivo '{}' subido exitosamente a MinIO en bucket '{}' como '{}'", filename, bucketName, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Error al subir archivo a MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Error al almacenar el archivo en MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFileUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String cleanEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return cleanEndpoint + "/" + bucketName + "/" + path;
    }

    @Override
    public String getPresignedDownloadUrl(String path, int duracionMinutos) {
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            int expiry = duracionMinutos > 0 ? duracionMinutos : 15;
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(path)
                            .expiry(expiry, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error al generar URL presignada para '{}': {}", path, e.getMessage(), e);
            throw new RuntimeException("Error al generar URL presignada en MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] downloadFile(String path) {
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(path)
                        .build())) {
            return is.readAllBytes();
        } catch (Exception e) {
            log.error("Error al descargar archivo de MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Error al descargar el archivo de MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
            log.info("Archivo '{}' eliminado de bucket '{}'", path, bucketName);
        } catch (Exception e) {
            log.error("Error al eliminar archivo de MinIO: {}", e.getMessage(), e);
            throw new RuntimeException("Error al eliminar el archivo de MinIO: " + e.getMessage(), e);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!found) {
            log.info("Bucket '{}' no existe en MinIO. Creando bucket...", bucketName);
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
            log.info("Bucket '{}' creado exitosamente", bucketName);
            setBucketPublicReadPolicy();
        }
    }

    private void setBucketPublicReadPolicy() throws Exception {
        String policy = String.format("""
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """, bucketName);
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build()
        );
        log.info("Política pública de lectura s3:GetObject configurada para el bucket '{}'", bucketName);
    }
}
