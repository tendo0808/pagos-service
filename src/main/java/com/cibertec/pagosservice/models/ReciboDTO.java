package com.cibertec.pagosservice.models;
import com.cibertec.pagosservice.entities.enums.TipoRecibo;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
public record ReciboDTO(Long id,String correlativo,@NotNull TipoRecibo tipo,LocalDateTime fecha,
 @NotNull @Positive BigDecimal monto,Long cuentaId,Long bancoId,@NotNull Long usuarioId,
 @Size(max=150) String depositante,@Size(max=100) String categoria,@Size(max=255) String concepto,LocalDate fechaDeposito) {}
