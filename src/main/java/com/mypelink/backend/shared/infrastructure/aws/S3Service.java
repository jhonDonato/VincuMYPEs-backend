package com.mypelink.backend.shared.infrastructure.aws;

import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;  // ✅ Cliente S3 v2

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    // ✅ MÉTODO CORREGIDO - Eliminar por key (nombre de archivo)
    public void deleteFile(String key) {
        try {
            // Verificar que la key no sea null o vacía
            if (key == null || key.isBlank()) {
                log.warn("Intento de eliminar archivo con key nula o vacía");
                return;
            }

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Archivo eliminado de S3: bucket={}, key={}", bucketName, key);
        } catch (Exception e) {
            log.error("Error eliminando archivo de S3: key={}, error={}", key, e.getMessage(), e);
            throw new BusinessException("Error al eliminar archivo de S3: " + e.getMessage());
        }
    }

    // ✅ MÉTODO CORREGIDO - Eliminar por URL completa
    public void eliminarArchivo(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isBlank()) {
                log.warn("Intento de eliminar archivo con URL nula o vacía");
                return;
            }

            String key = extractKeyFromUrl(fileUrl);
            deleteFile(key);
        } catch (Exception e) {
            log.error("Error eliminando archivo por URL: {}", e.getMessage(), e);
            throw new BusinessException("Error al eliminar archivo de S3: " + e.getMessage());
        }
    }

    // ✅ MÉTODO CORREGIDO - Extrae la key de la URL correctamente
    private String extractKeyFromUrl(String fileUrl) {
        try {
            // Decodificar la URL primero
            String decodedUrl = URLDecoder.decode(fileUrl, StandardCharsets.UTF_8.name());

            // Formato: https://bucket-name.s3.region.amazonaws.com/path/to/file.pdf
            String bucketWithDot = bucketName + ".s3.";
            int bucketIndex = decodedUrl.indexOf(bucketWithDot);

            if (bucketIndex != -1) {
                String afterBucket = decodedUrl.substring(bucketIndex + bucketWithDot.length());
                // Saltar la región y .amazonaws.com/
                int regionEnd = afterBucket.indexOf(".amazonaws.com/");
                if (regionEnd != -1) {
                    afterBucket = afterBucket.substring(regionEnd + 15);
                }
                return afterBucket;
            }

            // Formato alternativo: https://s3.region.amazonaws.com/bucket-name/path
            String s3Url = "s3." + region + ".amazonaws.com/";
            int s3Index = decodedUrl.indexOf(s3Url);
            if (s3Index != -1) {
                String afterS3 = decodedUrl.substring(s3Index + s3Url.length());
                if (afterS3.startsWith(bucketName + "/")) {
                    return afterS3.substring(bucketName.length() + 1);
                }
                return afterS3;
            }

            // Formato alternativo: solo la key directamente
            if (!decodedUrl.contains("://")) {
                return decodedUrl;
            }

            log.warn("No se pudo extraer la key de la URL: {}", fileUrl);
            throw new BusinessException("No se pudo extraer la key de la URL: " + fileUrl);

        } catch (Exception e) {
            log.error("Error extrayendo key de URL: {}", e.getMessage(), e);
            throw new BusinessException("Error al procesar la URL del archivo: " + e.getMessage());
        }
    }

    // ✅ MÉTODO CORREGIDO - Subir entregable PDF
    public String subirEntregablePdf(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("El archivo adjunto está vacío.");
        }

        // Validar tamaño máximo (5 MB)
        long maxBytes = 5L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("El archivo no puede superar los 5 MB.");
        }

        // Validar tipo MIME permitido
        String contentType = file.getContentType();
        boolean esPdf = "application/pdf".equalsIgnoreCase(contentType);
        boolean esWord = "application/msword".equalsIgnoreCase(contentType)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(contentType);
        boolean esTxt = "text/plain".equalsIgnoreCase(contentType);
        boolean esPpt = "application/vnd.ms-powerpoint".equalsIgnoreCase(contentType)
                || "application/vnd.openxmlformats-officedocument.presentationml.presentation".equalsIgnoreCase(contentType);

        if (!esPdf && !esWord && !esTxt && !esPpt) {
            throw new BusinessException("Formato no permitido. Solo se aceptan: PDF, Word, TXT y PowerPoint.");
        }

        // Generar nombre único
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = "entregables/" + UUID.randomUUID() + "_" + originalName.replace(" ", "_");

        try {
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + fileName;

        } catch (IOException e) {
            log.error("Error procesando archivo: {}", e.getMessage(), e);
            throw new BusinessException("Ocurrió un error al procesar el archivo.");
        } catch (Exception e) {
            log.error("Error subiendo a S3: {}", e.getMessage(), e);
            throw new BusinessException("Error de conexión con AWS S3: " + e.getMessage());
        }
    }

    // ✅ MÉTODO CORREGIDO - Subir CV PDF
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
            log.error("Error procesando CV: {}", e.getMessage(), e);
            throw new BusinessException("Ocurrió un error al procesar el CV.");
        } catch (Exception e) {
            log.error("Error subiendo CV a S3: {}", e.getMessage(), e);
            throw new BusinessException("Error al subir el CV a AWS S3: " + e.getMessage());
        }
    }

    // ✅ MÉTODO CORREGIDO - Subir insumo
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
            log.error("Error procesando insumo: {}", e.getMessage(), e);
            throw new BusinessException("Ocurrió un error al procesar el archivo.");
        } catch (Exception e) {
            log.error("Error subiendo insumo a S3: {}", e.getMessage(), e);
            throw new BusinessException("Error de conexión con AWS S3: " + e.getMessage());
        }
    }

    // ✅ MÉTODO CORREGIDO - Subir certificado
    public String subirCertificado(byte[] content, String key) {
        try {
            if (content == null || content.length == 0) {
                throw new BusinessException("El contenido del certificado está vacío");
            }

            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/pdf")
                    .build();

            s3Client.putObject(putOb, RequestBody.fromBytes(content));

            String url = "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
            log.info("Certificado subido a S3: {}", url);
            return url;

        } catch (Exception e) {
            log.error("Error subiendo certificado a S3: {}", e.getMessage(), e);
            throw new BusinessException("Error al subir certificado a S3: " + e.getMessage());
        }
    }

    // ✅ MÉTODO CORREGIDO - Subir imagen de perfil
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
            log.error("Error procesando imagen de perfil: {}", e.getMessage(), e);
            throw new BusinessException("Ocurrió un error al procesar la imagen de perfil.");
        } catch (Exception e) {
            log.error("Error subiendo imagen a S3: {}", e.getMessage(), e);
            throw new BusinessException("Error de conexión con AWS S3: " + e.getMessage());
        }
    }
    // En S3Service.java, agrega este método:
    public byte[] descargarArchivo(String key) {
        try {
            if (key == null || key.isBlank()) {
                throw new BusinessException("La key del archivo no puede estar vacía");
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);

            log.info("Archivo descargado de S3: bucket={}, key={}, size={}bytes",
                    bucketName, key, objectBytes.asByteArray().length);

            return objectBytes.asByteArray();
        } catch (S3Exception e) {
            log.error("Error de S3 al descargar archivo: key={}, error={}", key, e.awsErrorDetails().errorMessage());
            throw new BusinessException("Error al descargar archivo de S3: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            log.error("Error inesperado al descargar archivo: key={}, error={}", key, e.getMessage());
            throw new BusinessException("Error al descargar archivo de S3");
        }
    }
    // En S3Service.java
    public String extraerKeyDeUrl(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isBlank()) {
                return null;
            }

            // Si ya es una key (no contiene http), devolverla directamente
            if (!fileUrl.contains("://")) {
                return fileUrl;
            }

            // Formato: https://bucket-name.s3.region.amazonaws.com/path/to/file.png
            String bucketUrl = bucketName + ".s3.";
            int bucketIndex = fileUrl.indexOf(bucketUrl);

            if (bucketIndex != -1) {
                String afterBucket = fileUrl.substring(bucketIndex + bucketUrl.length());
                // Saltar la región y .amazonaws.com/
                int regionEnd = afterBucket.indexOf(".amazonaws.com/");
                if (regionEnd != -1) {
                    return afterBucket.substring(regionEnd + 15);
                }
            }

            // Formato alternativo: https://s3.region.amazonaws.com/bucket-name/path
            String s3Url = "s3." + region + ".amazonaws.com/";
            int s3Index = fileUrl.indexOf(s3Url);
            if (s3Index != -1) {
                String afterS3 = fileUrl.substring(s3Index + s3Url.length());
                if (afterS3.startsWith(bucketName + "/")) {
                    return afterS3.substring(bucketName.length() + 1);
                }
            }

            // Si no se puede extraer, intentar obtener la última parte de la URL
            java.net.URL url = new java.net.URL(fileUrl);
            String path = url.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            return path;
        } catch (Exception e) {
            log.error("Error extrayendo key de URL: {}", fileUrl, e);
            return null;
        }
    }
}