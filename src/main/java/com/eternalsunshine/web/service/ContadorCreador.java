package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.ContadorDocumento;
import com.eternalsunshine.web.repository.ContadorDocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ContadorCreador {

    @Autowired
    private ContadorDocumentoRepository repositorio;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void crearSiNoExiste(String tipo, String periodo) {

        if (repositorio.findByTipoAndPeriodo(tipo, periodo).isPresent()) {
            return;
        }

        ContadorDocumento contador = new ContadorDocumento();
        contador.setTipo(tipo);
        contador.setPeriodo(periodo);
        contador.setUltimo(0);

        repositorio.saveAndFlush(contador);
    }
}