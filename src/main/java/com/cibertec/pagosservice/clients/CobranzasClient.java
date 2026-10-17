package com.cibertec.pagosservice.clients;
import com.cibertec.pagosservice.models.CuentaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@FeignClient(name="cobranzas-service")
public interface CobranzasClient {
 @GetMapping("/api/cuentas-por-cobrar/{id}") CuentaDTO buscar(@PathVariable("id") Long id);
 @GetMapping("/api/cuentas-por-cobrar/socio/{id}") List<CuentaDTO> porSocio(@PathVariable("id") Long id);
 @GetMapping("/api/cuentas-por-cobrar/puesto/{id}") List<CuentaDTO> porPuesto(@PathVariable("id") Long id);
}
