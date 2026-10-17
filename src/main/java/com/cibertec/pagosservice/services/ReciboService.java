package com.cibertec.pagosservice.services;
import com.cibertec.pagosservice.clients.*;
import com.cibertec.pagosservice.entities.*;
import com.cibertec.pagosservice.entities.enums.TipoRecibo;
import com.cibertec.pagosservice.models.*;
import com.cibertec.pagosservice.repositories.*;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Service
public class ReciboService {
 private final ReciboRepository repo;
 private final BancoService bancos;
 private final CobranzasClient cobranzas;
 private final AuthClient auth;
 private final JdbcTemplate jdbc;
 public ReciboService(ReciboRepository repo,BancoService bancos,CobranzasClient cobranzas,AuthClient auth,JdbcTemplate jdbc){
  this.repo=repo;this.bancos=bancos;this.cobranzas=cobranzas;this.auth=auth;this.jdbc=jdbc;
 }
 @Transactional(readOnly=true) public ReciboDTO buscar(Long id){return dto(repo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Recibo no encontrado")));}
 @Transactional(readOnly=true) public List<ReciboDTO> listar(TipoRecibo tipo,LocalDateTime desde,LocalDateTime hasta){return repo.findByTipoAndFechaBetween(tipo,desde,hasta).stream().map(this::dto).toList();}
 @CircuitBreaker(name="consultaCobranzas",fallbackMethod="sociosFallback")
 public List<ReciboDTO> porSocio(Long socioId){return porCuentas(cobranzas.porSocio(socioId));}
 private List<ReciboDTO> sociosFallback(Long socioId,Throwable error){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No es posible consultar recibos del socio",error);}
 @CircuitBreaker(name="consultaCobranzas",fallbackMethod="puestosFallback")
 public List<ReciboDTO> porPuesto(Long puestoId){return porCuentas(cobranzas.porPuesto(puestoId));}
 private List<ReciboDTO> puestosFallback(Long puestoId,Throwable error){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No es posible consultar recibos del puesto",error);}
 private List<ReciboDTO> porCuentas(List<CuentaDTO> cuentas){List<Long> ids=cuentas.stream().map(CuentaDTO::id).toList();return ids.isEmpty()?List.of():repo.findByCuentaIdIn(ids).stream().map(this::dto).toList();}
 @Transactional
 public ReciboDTO ingresoExterno(ReciboDTO d){
  if(d.usuarioId()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Usuario obligatorio");
  verificarUsuario(d.usuarioId());
  if(d.monto()==null || d.monto().signum()<=0)
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El monto debe ser mayor a cero");
  return dto(repo.save(nuevo(TipoRecibo.INGRESO,d.monto(),null,null,d.usuarioId(),d.depositante(),d.categoria(),d.concepto(),null)));
 }
 @Transactional
 public List<ReciboDTO> procesar(ProcesarPagoRequest request){
  if(request==null || request.usuarioId()==null || request.cuentaIds()==null || request.cuentaIds().isEmpty())
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Se requieren usuario y cuentas");
  verificarUsuario(request.usuarioId());
  List<ReciboDTO> resultado=new ArrayList<>();
  for(Long id:new LinkedHashSet<>(request.cuentaIds())){
   CuentaDTO c=consultarCuenta(id);
   if(!"ABONADA".equals(c.estado()) || c.fechaPago()==null) continue;
   if(repo.existsByCuentaIdAndTipo(id,TipoRecibo.INGRESO) || repo.existsByCuentaIdAndTipo(id,TipoRecibo.BANCO))continue;
   // Emitir recibo solo para cuentas abonadas con fecha de pago confirmada.
   resultado.add(dto(repo.save(nuevo(TipoRecibo.INGRESO,c.monto(),id,null,request.usuarioId(),null,null,null,null))));
  }
  if(resultado.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"No hay cuentas abonadas con fecha de pago y sin recibo para procesar");
  return resultado;
 }
 @Transactional
 public ReciboDTO canjear(CanjeBancoRequest r){
  if(r==null || r.usuarioId()==null || r.bancoId()==null || r.cuentaId()==null)
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Se requieren usuario, banco y cuenta");
  verificarUsuario(r.usuarioId());
  Banco b=bancos.entidad(r.bancoId());
  CuentaDTO c=consultarCuenta(r.cuentaId());
  if(c.socioId()==null || !"PENDIENTE".equals(c.estado()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo se canjean cuentas de socio pendientes");
  if(repo.existsByCuentaIdAndTipo(r.cuentaId(),TipoRecibo.BANCO))throw new ResponseStatusException(HttpStatus.CONFLICT,"La cuenta ya tiene recibo bancario");
  // EXCEPCION ACADEMICA: al compartir una sola BD, el cambio de estado
  // y el recibo se confirman en UNA transaccion local, evitando pagos parciales.
  // La condicion WHERE previene dos canjes simultaneos de la misma cuenta.
  int actualizadas=jdbc.update(
    "UPDATE cuentas_por_cobrar SET estado = 'ABONADA', fecha_pago = CURRENT_DATE " +
    "WHERE id = ? AND estado = 'PENDIENTE' AND socio_id IS NOT NULL",r.cuentaId());
  if(actualizadas!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"La cuenta ya fue abonada o modificada");
  Recibo recibo=nuevo(TipoRecibo.BANCO,c.monto(),r.cuentaId(),b,r.usuarioId(),null,null,null,r.fechaDeposito());
  return dto(repo.save(recibo));
 }
 @CircuitBreaker(name="consultaCobranzas",fallbackMethod="cuentaFallback")
 public CuentaDTO consultarCuenta(Long id){return cobranzas.buscar(id);}
 private CuentaDTO cuentaFallback(Long id,Throwable error){
  if(error instanceof FeignException.NotFound)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Cuenta no encontrada",error);
  throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cobranzas no disponible",error);
 }
 private void verificarUsuario(Long id){
  try {if(!Boolean.TRUE.equals(auth.buscar(id).activo())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Usuario inactivo");} catch(FeignException.NotFound e){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuario no encontrado",e);}
  catch(FeignException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Auth service no disponible",e);}
 }
 private String correlativo(TipoRecibo tipo){
  // Identificador resistente a concurrencia; no depende de MAX(id).
  String prefijo=switch(tipo){case INGRESO->"ING";case EGRESO->"EGR";case BANCO->"BAN";};
  return prefijo+"-"+UUID.randomUUID().toString().replace("-","").substring(0,20);
 }
 private Recibo nuevo(TipoRecibo tipo,BigDecimal monto,Long cuentaId,Banco banco,Long usuarioId,String depositante,String categoria,String concepto,LocalDate deposito){
  Recibo r=new Recibo();r.setCorrelativo(correlativo(tipo));r.setTipo(tipo);r.setMonto(monto);r.setFecha(LocalDateTime.now());r.setCuentaId(cuentaId);r.setBanco(banco);
  r.setUsuarioId(usuarioId);r.setDepositante(depositante);r.setCategoria(categoria);r.setConcepto(concepto);r.setFechaDeposito(deposito);return r;
 }
 private ReciboDTO dto(Recibo r){return new ReciboDTO(r.getId(),r.getCorrelativo(),r.getTipo(),r.getFecha(),r.getMonto(),r.getCuentaId(),r.getBanco()==null?null:r.getBanco().getId(),r.getUsuarioId(),r.getDepositante(),r.getCategoria(),r.getConcepto(),r.getFechaDeposito());}
}
