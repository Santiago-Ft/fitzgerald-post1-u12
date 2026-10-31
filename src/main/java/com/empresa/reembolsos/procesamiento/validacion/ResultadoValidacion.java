package com.empresa.reembolsos.procesamiento.validacion;

/**
 * Representa el resultado inmutable de una etapa de validacion sobre una solicitud.
 */
public record ResultadoValidacion(boolean valida, String motivoRechazo) {
    public static ResultadoValidacion ok() {
        return new ResultadoValidacion(true, null);
    }

    public static ResultadoValidacion rechazar(String motivo) {
        return new ResultadoValidacion(false, motivo);
    }
}
