package com.cibertec.pagosservice.repositories;
import com.cibertec.pagosservice.entities.Recibo;
import com.cibertec.pagosservice.entities.enums.TipoRecibo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;
public interface ReciboRepository extends JpaRepository<Recibo,Long> {
 Optional<Recibo> findTopByTipoOrderByIdDesc(TipoRecibo tipo);
 List<Recibo> findByTipoAndFechaBetween(TipoRecibo tipo,LocalDateTime desde,LocalDateTime hasta);
 List<Recibo> findByCuentaIdIn(Collection<Long> cuentaIds);
 boolean existsByCuentaIdAndTipo(Long cuentaId,TipoRecibo tipo);
}
