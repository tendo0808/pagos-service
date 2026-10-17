package com.cibertec.pagosservice.repositories;
import com.cibertec.pagosservice.entities.Banco;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BancoRepository extends JpaRepository<Banco,Long> {}
