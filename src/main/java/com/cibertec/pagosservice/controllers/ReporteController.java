package com.cibertec.pagosservice.controllers;

import com.cibertec.pagosservice.services.ReporteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {
    private static final MediaType XLSX=MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final ReporteService service;
    public ReporteController(ReporteService service){this.service=service;}

    @GetMapping("/movimientos-diarios")
    public ResponseEntity<byte[]> movimientosDiarios(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fecha){
        return descargar(service.movimientosDiarios(fecha),"movimientos-diarios-"+fecha+".xlsx");
    }
    @GetMapping("/bancos")
    public ResponseEntity<byte[]> bancos(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fecha){
        return descargar(service.bancos(fecha),"bancos-"+fecha+".xlsx");
    }
    @GetMapping("/totales")
    public ResponseEntity<byte[]> totales(@RequestParam String mes){return descargar(service.totales(parsearMes(mes)),"totales-"+mes+".xlsx");}
    @GetMapping("/mensual")
    public ResponseEntity<byte[]> mensual(@RequestParam String mes){return descargar(service.mensual(parsearMes(mes)),"mensual-"+mes+".xlsx");}
    @GetMapping("/socios")
    public ResponseEntity<byte[]> socios(@RequestParam String mes){return descargar(service.socios(parsearMes(mes)),"socios-"+mes+".xlsx");}
    @GetMapping("/no-socios")
    public ResponseEntity<byte[]> noSocios(@RequestParam String mes){return descargar(service.noSocios(parsearMes(mes)),"no-socios-"+mes+".xlsx");}
    @GetMapping("/egresos")
    public ResponseEntity<byte[]> egresos(@RequestParam String mes){return descargar(service.egresos(parsearMes(mes)),"egresos-"+mes+".xlsx");}

    private YearMonth parsearMes(String texto) {
        try { return YearMonth.parse(texto); }
        catch (DateTimeParseException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mes invalido; use yyyy-MM",e); }
    }

    private ResponseEntity<byte[]> descargar(byte[] content,String filename){
        return ResponseEntity.ok().contentType(XLSX).contentLength(content.length)
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(filename).build().toString())
            .body(content);
    }
}
