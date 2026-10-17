package com.cibertec.pagosservice.models;
import java.math.BigDecimal;
import java.time.LocalDate;
public record CuentaDTO(Long id,Long servicioId,Long socioId,Long puestoId,String periodo,BigDecimal monto,
 String estado,BigDecimal lecturaInicial,BigDecimal lecturaFinal,LocalDate fechaGeneracion,LocalDate fechaPago) {}
