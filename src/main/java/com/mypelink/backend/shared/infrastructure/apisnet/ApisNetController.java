package com.mypelink.backend.shared.infrastructure.apisnet;

import com.mypelink.backend.shared.infrastructure.apisnet.dto.DniResponseDto;
import com.mypelink.backend.shared.infrastructure.apisnet.dto.RucResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reniec")
@RequiredArgsConstructor
public class ApisNetController {

    private final ApisNetService apisNetService;

    @GetMapping("/dni/{dni}")
    public DniResponseDto buscarDni(@PathVariable String dni) {

        if (dni.length() != 8) {
            throw new RuntimeException("El DNI debe tener 8 dígitos");
        }

        return apisNetService.buscarPorDni(dni);
    }
    @GetMapping("/ruc/{ruc}")
    public RucResponseDto buscarRuc(
            @PathVariable String ruc
    ) {

        if (ruc.length() != 11) {

            throw new RuntimeException(
                    "El RUC debe tener 11 dígitos"
            );
        }

        return apisNetService.buscarPorRuc(ruc);
    }
}
