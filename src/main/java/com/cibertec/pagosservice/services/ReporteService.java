package com.cibertec.pagosservice.services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** Reportes academicos: lectura de la base compartida, sin duplicar entidades JPA de otros servicios. */
@Service
@Transactional(readOnly = true)
public class ReporteService {
    private final JdbcTemplate jdbc;
    public ReporteService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final String RECIBOS = "SELECT r.correlativo, r.tipo, r.fecha, r.monto, r.cuenta_id, " +
        "COALESCE(b.nombre,'-') AS banco, COALESCE(CONCAT(u.nombres,' ',u.apellidos),'-') AS usuario, " +
        "COALESCE(r.concepto,'-') AS detalle, r.fecha_deposito " +
        "FROM recibos r LEFT JOIN bancos b ON b.id=r.banco_id " +
        "LEFT JOIN usuarios u ON u.id=r.usuario_id ";

    public byte[] movimientosDiarios(LocalDate fecha) {
        List<Map<String,Object>> recibos = jdbc.queryForList(RECIBOS +
            "WHERE r.fecha >= ? AND r.fecha < ? ORDER BY r.fecha, r.id", fecha.atStartOfDay(),fecha.plusDays(1).atStartOfDay());
        List<Map<String,Object>> egresos = egresosEntre(fecha,fecha.plusDays(1));
        try (Workbook wb = new XSSFWorkbook()) {
            crearHoja(wb,"Ingresos y bancos", new String[]{"Correlativo","Tipo","Fecha","Monto","Cuenta","Banco","Usuario","Detalle"},
                recibos,"correlativo","tipo","fecha","monto","cuenta_id","banco","usuario","detalle");
            crearHoja(wb,"Egresos",new String[]{"Numero documento","Proveedor","Fecha","Importe","Estado","Motivo"},
                egresos,"numero_documento","proveedor","fecha","importe","estado","motivo");
            return bytes(wb);
        } catch(IOException ex) {throw new IllegalStateException("Error generando XLSX",ex);}
    }
    public byte[] bancos(LocalDate fecha) {
        List<Map<String,Object>> data = jdbc.queryForList(RECIBOS +
            "WHERE r.tipo='BANCO' AND r.fecha >= ? AND r.fecha < ? ORDER BY r.fecha, r.id",
            fecha.atStartOfDay(),fecha.plusDays(1).atStartOfDay());
        try(Workbook wb=new XSSFWorkbook()) {
            crearHoja(wb,"Recibos bancarios",new String[]{"Correlativo","Banco","Cuenta","Monto","Fecha deposito","Usuario"},
                data,"correlativo","banco","cuenta_id","monto","fecha_deposito","usuario");
            return bytes(wb);
        } catch(IOException ex){throw new IllegalStateException("Error generando XLSX",ex);}
    }
    public byte[] totales(YearMonth mes) {
        var recibos=recibosMes(mes);
        var egresos=egresosEntre(mes.atDay(1),mes.plusMonths(1).atDay(1));
        BigDecimal ingresos=sumarRecibos(recibos,"INGRESO");
        BigDecimal bancos=sumarRecibos(recibos,"BANCO");
        BigDecimal gastos=egresos.stream().map(x -> new BigDecimal(String.valueOf(x.get("importe"))))
                .reduce(BigDecimal.ZERO,BigDecimal::add);
        try(Workbook wb=new XSSFWorkbook()){
            List<Map<String,Object>> data=List.of(
                Map.of("concepto","Ingresos (cobranza)","total",ingresos),
                Map.of("concepto","Canjes bancarios","total",bancos),
                Map.of("concepto","Egresos","total",gastos),
                Map.of("concepto","Neto (ingresos + banco - egresos)","total",ingresos.add(bancos).subtract(gastos)));
            crearHoja(wb,"Totales " + mes,new String[]{"Concepto","Total"},data,"concepto","total");
            return bytes(wb);
        }catch(IOException ex){throw new IllegalStateException("Error generando XLSX",ex);}
    }
    public byte[] mensual(YearMonth mes){
        try(Workbook wb=new XSSFWorkbook()){
            crearHoja(wb,"Recibos " + mes,new String[]{"Correlativo","Tipo","Fecha","Monto","Cuenta","Banco","Usuario","Detalle"},
                recibosMes(mes),"correlativo","tipo","fecha","monto","cuenta_id","banco","usuario","detalle");
            return bytes(wb);
        }catch(IOException ex){throw new IllegalStateException("Error generando XLSX",ex);}
    }
    public byte[] socios(YearMonth mes){return cuentas(mes,true);}
    public byte[] noSocios(YearMonth mes){return cuentas(mes,false);}
    private byte[] cuentas(YearMonth mes,boolean esSocio){
        String sql="SELECT c.id, COALESCE(CONCAT(s.nombres,' ',s.apellidos),'-') AS socio, " +
            "COALESCE(s.codigo,'-') AS codigo, COALESCE(p.numero,'-') AS puesto, " +
            "sv.nombre AS servicio,c.periodo,c.monto,c.estado,c.fecha_generacion,c.fecha_pago " +
            "FROM cuentas_por_cobrar c JOIN servicios sv ON sv.id=c.servicio_id " +
            "LEFT JOIN socios s ON s.id=c.socio_id LEFT JOIN puestos p ON p.id=c.puesto_id " +
            "WHERE " + (esSocio ? "c.socio_id IS NOT NULL " : "c.socio_id IS NULL AND c.puesto_id IS NOT NULL ") +
            "AND c.fecha_generacion >= ? AND c.fecha_generacion < ? ORDER BY c.id";
        var rows=jdbc.queryForList(sql,mes.atDay(1),mes.plusMonths(1).atDay(1));
        try(Workbook wb=new XSSFWorkbook()){
            if(esSocio)crearHoja(wb,"Socios " + mes,new String[]{"Socio","Codigo","Servicio","Periodo","Monto","Estado","Fecha generacion","Fecha pago"},
                rows,"socio","codigo","servicio","periodo","monto","estado","fecha_generacion","fecha_pago");
            else crearHoja(wb,"No socios " + mes,new String[]{"Puesto","Servicio","Periodo","Monto","Estado","Fecha generacion","Fecha pago"},
                rows,"puesto","servicio","periodo","monto","estado","fecha_generacion","fecha_pago");
            return bytes(wb);
        }catch(IOException ex){throw new IllegalStateException("Error generando XLSX",ex);}
    }
    public byte[] egresos(YearMonth mes){
        try(Workbook wb=new XSSFWorkbook()){
            crearHoja(wb,"Egresos " + mes,new String[]{"Numero documento","Proveedor","Fecha","Importe","Estado","Motivo","Usuario"},
                egresosEntre(mes.atDay(1),mes.plusMonths(1).atDay(1)),
                "numero_documento","proveedor","fecha","importe","estado","motivo","usuario");
            return bytes(wb);
        }catch(IOException ex){throw new IllegalStateException("Error generando XLSX",ex);}
    }
    private List<Map<String,Object>> recibosMes(YearMonth mes){
        return jdbc.queryForList(RECIBOS + "WHERE r.fecha >= ? AND r.fecha < ? ORDER BY r.fecha, r.id",
            mes.atDay(1).atStartOfDay(),mes.plusMonths(1).atDay(1).atStartOfDay());
    }
    private List<Map<String,Object>> egresosEntre(LocalDate inicio,LocalDate exclusivo){
        return jdbc.queryForList("SELECT e.numero_documento,e.proveedor,e.fecha,e.importe,e.estado,e.motivo, " +
            "COALESCE(CONCAT(u.nombres,' ',u.apellidos),'-') AS usuario " +
            "FROM egresos e LEFT JOIN usuarios u ON u.id=e.usuario_id " +
            "WHERE e.fecha >= ? AND e.fecha < ? ORDER BY e.fecha,e.id",inicio,exclusivo);
    }
    private BigDecimal sumarRecibos(List<Map<String,Object>> rows,String tipo){
        return rows.stream().filter(r -> tipo.equals(String.valueOf(r.get("tipo"))))
            .map(r -> new BigDecimal(String.valueOf(r.get("monto")))).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    private void crearHoja(Workbook wb,String nombre,String[] titulos,List<Map<String,Object>> datos,String... keys){
        Sheet hoja=wb.createSheet(nombre.length()>31?nombre.substring(0,31):nombre);
        CellStyle estilo=wb.createCellStyle();Font font=wb.createFont();font.setBold(true);estilo.setFont(font);
        Row titulo=hoja.createRow(0);
        for(int i=0;i<titulos.length;i++){
            Cell celda=titulo.createCell(i);celda.setCellValue(titulos[i]);celda.setCellStyle(estilo);
            hoja.setColumnWidth(i,24*256);
        }
        int fila=1;
        for(Map<String,Object> dato:datos){
            Row row=hoja.createRow(fila++);
            for(int i=0;i<keys.length;i++){
                Object val=dato.get(keys[i]);Cell cell=row.createCell(i);
                if(val==null) cell.setCellValue("-");
                else if(val instanceof Number num)cell.setCellValue(num.doubleValue());
                else cell.setCellValue(String.valueOf(val));
            }
        }
    }
    private byte[] bytes(Workbook wb)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();wb.write(out);return out.toByteArray();
    }
}
