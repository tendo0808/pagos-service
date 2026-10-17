package com.cibertec.pagosservice.entities;
import com.cibertec.pagosservice.entities.enums.TipoRecibo;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
@Entity @Table(name="recibos", uniqueConstraints={@UniqueConstraint(name="uk_recibo_cuenta",columnNames={"cuenta_id"})}) @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Recibo {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=30) private String correlativo;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private TipoRecibo tipo;
 @Column(nullable=false) private LocalDateTime fecha;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal monto;
 @Column(name="cuenta_id") private Long cuentaId;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="banco_id") private Banco banco;
 @Column(name="usuario_id",nullable=false) private Long usuarioId;
 @Column(length=150) private String depositante;
 @Column(length=100) private String categoria;
 @Column(length=255) private String concepto;
 @Column(name="fecha_deposito") private LocalDate fechaDeposito;
}
