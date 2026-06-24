package com.mypelink.backend.shared.infrastructure.exception;

public class MypeEstadoException extends RuntimeException {
    private final String codigo;

    public MypeEstadoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
