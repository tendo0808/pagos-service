package com.cibertec.pagosservice.models;
import jakarta.validation.constraints.*;
import java.util.List;
public record ProcesarPagoRequest(@NotEmpty List<@NotNull Long> cuentaIds,@NotNull Long usuarioId) {}
