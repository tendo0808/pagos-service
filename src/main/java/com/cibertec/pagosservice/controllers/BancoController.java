package com.cibertec.pagosservice.controllers;
import com.cibertec.pagosservice.models.BancoDTO;
import com.cibertec.pagosservice.services.BancoService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/bancos")
public class BancoController {
 private final BancoService svc;
 public BancoController(BancoService svc){this.svc=svc;}
 @GetMapping public List<BancoDTO> listar(){return svc.listar();}
 @GetMapping("/{id}") public BancoDTO buscar(@PathVariable Long id){return svc.buscar(id);}
 @PostMapping public ResponseEntity<BancoDTO> crear(@Valid @RequestBody BancoDTO dto){return ResponseEntity.status(HttpStatus.CREATED).body(svc.crear(dto));}
 @PutMapping("/{id}") public BancoDTO editar(@PathVariable Long id,@Valid @RequestBody BancoDTO dto){return svc.editar(id,dto);}
 @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id){svc.eliminar(id);return ResponseEntity.noContent().build();}
}
