package com.cibertec.pagosservice.controllers;
import com.cibertec.pagosservice.entities.enums.TipoRecibo;
import com.cibertec.pagosservice.models.*;
import com.cibertec.pagosservice.services.ReciboService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
@RestController @RequestMapping("/api/recibos")
public class ReciboController {
 private final ReciboService svc;
 public ReciboController(ReciboService svc){this.svc=svc;}
 @PostMapping("/pagos") public ResponseEntity<List<ReciboDTO>> pagar(@Valid @RequestBody ProcesarPagoRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(svc.procesar(r));}
 @PostMapping("/canje") public ResponseEntity<ReciboDTO> canjear(@Valid @RequestBody CanjeBancoRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(svc.canjear(r));}
 @PostMapping("/ingresos-externos") public ResponseEntity<ReciboDTO> ingreso(@Valid @RequestBody ReciboDTO r){return ResponseEntity.status(HttpStatus.CREATED).body(svc.ingresoExterno(r));}
 @GetMapping public List<ReciboDTO> listar(@RequestParam TipoRecibo tipo,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta){return svc.listar(tipo,desde,hasta);}
 @GetMapping("/socio/{id}") public List<ReciboDTO> socio(@PathVariable Long id){return svc.porSocio(id);}
 @GetMapping("/puesto/{id}") public List<ReciboDTO> puesto(@PathVariable Long id){return svc.porPuesto(id);}
 @GetMapping("/{id}") public ReciboDTO buscar(@PathVariable Long id){return svc.buscar(id);}
}
