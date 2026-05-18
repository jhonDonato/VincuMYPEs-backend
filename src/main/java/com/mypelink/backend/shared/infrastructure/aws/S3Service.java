package com.mypelink.backend.shared.infrastructure.aws;

import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    public String subirEntregablePdf(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("El archivo adjunto está vacío.");
        }

        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new BusinessException("Formato no válido. Por seguridad, los entregables solo pueden ser archivos PDF.");
        }

        String fileName = "entregables/" + UUID.randomUUID() + "_" + file.getOriginalFilename().replace(" ", "_");

        try {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType("application/pdf")
                    .build();

            s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + fileName;

        } catch (IOException e) {
            throw new BusinessException("Ocurrió un error al procesar el archivo PDF.");
        } catch (Exception e) {
            throw new BusinessException("Error de conexión con AWS S3: " + e.getMessage());
        }
    }

    public String subirImagenPerfil(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("La imagen está vacía.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("Formato no válido. Solo se permiten imágenes (JPG, PNG, WEBP).");
        }

        String fileName = "perfiles/" + UUID.randomUUID() + "_" + file.getOriginalFilename().replace(" ", "_");

        try {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + fileName;

        } catch (IOException e) {
            throw new BusinessException("Ocurrió un error al procesar la imagen de perfil.");
        } catch (Exception e) {
            throw new BusinessException("Error de conexión con AWS S3: " + e.getMessage());
        }
    }
}