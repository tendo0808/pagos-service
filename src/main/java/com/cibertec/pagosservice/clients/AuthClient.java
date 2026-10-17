package com.cibertec.pagosservice.clients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name="auth-service")
public interface AuthClient {
 record UsuarioInfo(Long id,Boolean activo) {}
 @GetMapping("/api/usuarios/{id}") UsuarioInfo buscar(@PathVariable("id") Long id);
}
