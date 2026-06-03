package com.mypelink.backend.shared.infrastructure.aws;

import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;  // ✅ Esto es SdkClient (v2)

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    // ✅ MÉTODO CORREGIDO - usa s3Client (no amazonS3)
    public void eliminarArchivo(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);

        } catch (Exception e) {
            throw new BusinessException("Error al eliminar archivo de S3: " + e.getMessage());
        }
    }

    // ✅ MÉTODO CORREGIDO - extrae la key correctamente
    private String extractKeyFromUrl(String fileUrl) {
        try {
            // La URL tiene formato: https://bucket-name.s3.region.amazonaws.com/path/to/file.pdf
            // Necesitamos extraer todo después del bucket

            String bucketWithDot = bucketName + ".s3.";

            int bucketIndex = fileUrl.indexOf(bucketWithDot);
            if (bucketIndex == -1) {
                // Intentar otro formato: https://s3.region.amazonaws.com/bucket-name/path
                bucketIndex = fileUrl.indexOf(bucketName);
                if (bucketIndex == -1) {
                    throw new BusinessException("No se pudo extraer la key de la URL: " + fileUrl);
                }
                String afterBucket = fileUrl.substring(bucketIndex + bucketName.length() + 1);
                // Decodificar URL (por si tiene espacios o caracteres especiales)
                return URLDecoder.decode(afterBucket, StandardCharsets.UTF_8.name());
            }

            String afterBucket = fileUrl.substring(bucketIndex + bucketWithDot.length());
            // Saltar la región y .amazonaws.com/
            int regionEnd = afterBucket.indexOf(".amazonaws.com/");
            if (regionEnd != -1) {
                afterBucket = afterBucket.substring(regionEnd + 15); // len(".amazonaws.com/") = 15
            }

            // Decodificar URL
            return URLDecoder.decode(afterBucket, StandardCharsets.UTF_8.name());

        } catch (Exception e) {
            throw new BusinessException("Error al procesar la URL del archivo: " + e.getMessage());
        }
    }

    // Tus métodos existentes (subirEntregablePdf, subirCvPdf, subirImagenPerfil) se quedan igual
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

    public String subirCvPdf(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("El archivo CV está vacío.");
        }
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new BusinessException("El CV debe ser un archivo PDF.");
        }

        long maxBytes = 5L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("El CV no puede superar los 5MB.");
        }

        String fileName = "cvs/" + UUID.randomUUID() + "_" + file.getOriginalFilename().replace(" ", "_");

        try {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType("application/pdf")
                    .build();

            s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + fileName;

        } catch (IOException e) {
            throw new BusinessException("Ocurrió un error al procesar el CV.");
        } catch (Exception e) {
            throw new BusinessException("Error al subir el CV a AWS S3: " + e.getMessage());
        }
    }

    public String subirInsumo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("El archivo está vacío.");
        }
        String fileName = "insumos/" + UUID.randomUUID() + "_" + file.getOriginalFilename().replace(" ", "_");
        try {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + fileName;
        } catch (IOException e) {
            throw new BusinessException("Ocurrió un error al procesar el archivo.");
        } catch (Exception e) {
            throw new BusinessException("Error de conexión con AWS S3: " + e.getMessage());
        }
    }
    public String subirCertificado(byte[] content, String key) {
        try {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/pdf")
                    .build();
            s3Client.putObject(putOb, RequestBody.fromBytes(content));
            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
        } catch (Exception e) {
            throw new BusinessException("Error al subir certificado a S3: " + e.getMessage());
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