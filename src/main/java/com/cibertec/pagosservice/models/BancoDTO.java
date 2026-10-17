package com.cibertec.pagosservice.models;
import com.cibertec.pagosservice.entities.enums.Moneda;
import jakarta.validation.constraints.*;
public record BancoDTO(Long id,@NotBlank @Size(max=100) String nombre,@NotBlank @Size(max=30) String numeroCuenta,
 @Size(max=30) String cci,@NotNull Moneda moneda) {}
