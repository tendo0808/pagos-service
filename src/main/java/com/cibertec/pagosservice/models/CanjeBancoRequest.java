package com.cibertec.pagosservice.models;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record CanjeBancoRequest(@NotNull Long cuentaId,@NotNull Long bancoId,@NotNull LocalDate fechaDeposito,@NotNull Long usuarioId) {}
