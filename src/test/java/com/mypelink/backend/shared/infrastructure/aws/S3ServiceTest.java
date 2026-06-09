package com.mypelink.backend.shared.infrastructure.aws;

import com.mypelink.backend.shared.infrastructure.aws.S3Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock private S3Client s3Client;

    @InjectMocks
    private S3Service s3Service;

    @Test
    void subirEntregablePdf_Success() throws IOException {
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "region", "us-east-1");

        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("doc.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(1024L);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));

        PutObjectResponse putResponse = PutObjectResponse.builder().build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(putResponse);

        String url = s3Service.subirEntregablePdf(file);

        assertNotNull(url);
        assertTrue(url.contains("test-bucket"));
    }
}
