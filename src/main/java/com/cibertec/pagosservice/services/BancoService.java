package com.cibertec.pagosservice.services;
import com.cibertec.pagosservice.entities.Banco;
import com.cibertec.pagosservice.models.BancoDTO;
import com.cibertec.pagosservice.repositories.BancoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@Service
public class BancoService {
 private final BancoRepository repo;
 public BancoService(BancoRepository repo){this.repo=repo;}
 @Transactional(readOnly=true) public List<BancoDTO> listar(){return repo.findAll().stream().map(this::dto).toList();}
 @Transactional(readOnly=true) public BancoDTO buscar(Long id){return dto(entidad(id));}
 @Transactional public BancoDTO crear(BancoDTO d){Banco b=new Banco(null,d.nombre(),d.numeroCuenta(),d.cci(),d.moneda());return dto(repo.save(b));}
 @Transactional public BancoDTO editar(Long id,BancoDTO d){Banco b=entidad(id);b.setNombre(d.nombre());b.setNumeroCuenta(d.numeroCuenta());b.setCci(d.cci());b.setMoneda(d.moneda());return dto(repo.save(b));}
 @Transactional public void eliminar(Long id){repo.delete(entidad(id));}
 public Banco entidad(Long id){return repo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Banco no encontrado"));}
 private BancoDTO dto(Banco b){return new BancoDTO(b.getId(),b.getNombre(),b.getNumeroCuenta(),b.getCci(),b.getMoneda());}
}
