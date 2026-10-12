package com.coresales.service.organizacion.repository;

import com.coresales.service.organizacion.model.PersonalUO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PersonalUORepositoryImpl
        implements PersonalUORepository {

    private static final String SP =
            "organizacion.dbo.spEO_Personal_BuscarPersonalxUO";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<PersonalUO> buscarPersonalxUO(
            String codigoUo,
            Short tipo
    ) {

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(SP);

        // Parámetros posicionales (@PNCODUO, @PTIPO): evita que el driver
        // tenga que leer la metadata del SP en otra base de datos.
        query.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter(2, Short.class, ParameterMode.IN);

        query.setParameter(1, codigoUo);
        query.setParameter(2, tipo);

        @SuppressWarnings("unchecked")
        List<Object[]> resultados =
                query.getResultList();

        List<PersonalUO> personal =
                new ArrayList<>();

        for (Object[] fila : resultados) {

            PersonalUO p = new PersonalUO();

            p.setCodigoPersonal(texto(fila[0]));   // 0  CCODPER
            p.setUsuarioWindows(texto(fila[1]));   // 1  VUSUWIN
            p.setDni(texto(fila[2]));              // 2  VNUMDNI
            p.setNombre(texto(fila[3]));           // 3  VNOMBRE
            p.setCodigoUo(entero(fila[4]));        // 4  NCODUO
            p.setUnidadOrganica(texto(fila[5]));   // 5  VDESLUO
            p.setCodigoCargo(entero(fila[6]));     // 6  NCODCAR
            p.setCargo(texto(fila[7]));            // 7  VDESCAR
            p.setEstado(texto(fila[8]));           // 8  VNOMEST
            p.setCategoria(texto(fila[9]));        // 9  VNOMCAT
            p.setCorreo(texto(fila[10]));          // 10 VCORREO
            p.setUbicacion(texto(fila[11]));       // 11 VNOMUBI

            Integer jefe = entero(fila[12]);       // 12 CCODJEF (COUNT)
            p.setCantidadUoJefe(jefe == null ? 0 : jefe);
            p.setJefe(jefe != null && jefe > 0);

            personal.add(p);
        }

        return personal;
    }

    private String texto(Object valor) {
        return valor == null ? null : valor.toString().trim();
    }

    private Integer entero(Object valor) {

        if (valor == null) {
            return null;
        }

        if (valor instanceof Number numero) {
            return numero.intValue();
        }

        String s = valor.toString().trim();
        return s.isEmpty() ? null : Integer.valueOf(s);
    }
}
