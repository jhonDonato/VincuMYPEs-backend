package com.mypelink.backend.shared.infrastructure.apisnet;

import com.mypelink.backend.shared.infrastructure.apisnet.dto.DniResponseDto;
import com.mypelink.backend.shared.infrastructure.apisnet.dto.RucResponseDto;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class ApisNetService {

    @Value("${API_TOKEN}")
    private String apiToken;

    private static final String BASE_URL =
            "https://peruapi.com/api/dni/";

    public DniResponseDto buscarPorDni(String dni) {

        try {

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();

            // AQUÍ ESTÁ LA CLAVE
            headers.set("X-API-KEY", apiToken);

            HttpEntity<String> entity =
                    new HttpEntity<>(headers);

            String url = BASE_URL + dni;

            System.out.println("URL: " + url);
            System.out.println("TOKEN: " + apiToken);

            ResponseEntity<DniResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            DniResponseDto.class
                    );

            DniResponseDto dto = response.getBody();

            // Agrega estos logs para verificar la respuesta
            System.out.println("Respuesta de PeruAPI: " + dto);
            if (dto != null) {
                System.out.println("Nombres: " + dto.getNombres());
                System.out.println("Apellido Paterno: " + dto.getApellido_paterno());
                System.out.println("Apellido Materno: " + dto.getApellido_materno());
            }

            return dto;


        } catch (Exception e) {

            e.printStackTrace();

            throw new BusinessException("Error consultando el RUC. Intenta nuevamente.");
        }
    }
    public RucResponseDto buscarPorRuc(String ruc) {

        try {

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();

            headers.set("X-API-KEY", apiToken);

            HttpEntity<String> entity =
                    new HttpEntity<>(headers);

            String url =
                    "https://peruapi.com/api/ruc/" + ruc;

            ResponseEntity<RucResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            RucResponseDto.class
                    );

            RucResponseDto dto = response.getBody();

            if (dto != null && !"ACTIVO".equalsIgnoreCase(dto.getEstado())) {
                throw new RuntimeException(
                        "Solo se permiten empresas con estado ACTIVO."
                );
            }

            return dto;

        } catch (Exception e) {

            e.printStackTrace();

            throw new BusinessException(
                    "Solo se permiten empresas con estado ACTIVO."
            );
        }
    }
}