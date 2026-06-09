package com.mypelink.backend.shared.infrastructure.apisnet;

import com.mypelink.backend.shared.infrastructure.apisnet.dto.DniResponseDto;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ApisNetServiceTest {

    private ApisNetService apisNetService;

    @BeforeEach
    void setUp() {
        apisNetService = new ApisNetService();
        ReflectionTestUtils.setField(apisNetService, "apiToken", "test-token");
    }

    @Test
    void buscarPorDni_ShouldFail_WithInvalidDni() {
        assertThrows(BusinessException.class,
                () -> apisNetService.buscarPorDni("00000000"));
    }

    @Test
    void buscarPorRuc_ShouldFail_WithInvalidRuc() {
        assertThrows(BusinessException.class,
                () -> apisNetService.buscarPorRuc("00000000000"));
    }
}
