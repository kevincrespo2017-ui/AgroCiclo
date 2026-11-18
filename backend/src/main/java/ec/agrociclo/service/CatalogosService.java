package ec.agrociclo.service;

import ec.agrociclo.model.*;
import ec.agrociclo.repository.*;
import ec.agrociclo.security.Permisos;

import ec.agrociclo.repository.BaseDatos;
import ec.agrociclo.exception.ErrorApi;
import ec.agrociclo.validation.Validaciones;
import ec.agrociclo.model.Usuario;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * CatalogosService: Implementa la lógica de negocio y las reglas del módulo.
 */
public class CatalogosService {
    private final TerrenoRepository terrenoRepository;
    private final CultivoRepository cultivoRepository;
    private final InsumoRepository insumoRepository;
    private final BaseDatos db;
    private final CatalogosRepository persistencia;

    public CatalogosService(BaseDatos db, TerrenoRepository terrenoRepository, CultivoRepository cultivoRepository, InsumoRepository insumoRepository, CatalogosRepository persistencia) {
        this.persistencia=persistencia;
        this.terrenoRepository=terrenoRepository;
        this.cultivoRepository=cultivoRepository;
        this.insumoRepository=insumoRepository; this.db = db; }

    public List<?> listar(TipoCatalogo tipo) {
        return switch(tipo) {
            case TERRENOS -> terrenoRepository.listar();
            case CULTIVOS -> cultivoRepository.listar();
            case INSUMOS -> insumoRepository.listar();
        };
    }

    @Transactional
    public Object crear(Usuario actor, TipoCatalogo tipo, Map<String, Object> datos) {
        Permisos.exigirAdminOSupervisor(actor);
        verificarUnidad(tipo, datos);
        var nuevo = persistencia.crear(tipo, datos);
        db.auditar(actor, "CREAR", tipo.tabla(), BaseDatos.entero(nuevo, "id"), datos);
        return modelo(tipo, BaseDatos.entero(nuevo, "id"));
    }

    @Transactional
    public Object editar(Usuario actor, TipoCatalogo tipo, int id, Map<String, Object> datos) {
        Permisos.exigirAdminOSupervisor(actor);
        var actual = persistencia.bloquear(tipo, Validaciones.id(id));
        verificarUnidad(tipo, datos);
        if (tipo == TipoCatalogo.TERRENOS) {
            var mayor = persistencia.areaMaxima(id);
            if (mayor.get("area") != null && BaseDatos.decimal(mayor, "area").compareTo((BigDecimal) datos.get("area_ha")) > 0) {
                throw ErrorApi.conflicto("AREA_EN_USO", "El área nueva es menor a la de un ciclo vinculado.");
            }
        }
        if (tipo == TipoCatalogo.INSUMOS && !actual.get("unidad").equals(datos.get("unidad")) &&
            persistencia.tieneMovimientos(id)) {
            throw ErrorApi.conflicto("UNIDAD_EN_USO", "El insumo ya tiene movimientos con la unidad original.");
        }
        var editado = persistencia.editar(tipo, id, datos);
        db.auditar(actor, "EDITAR", tipo.tabla(), id, datos);
        return modelo(tipo, BaseDatos.entero(editado, "id"));
    }

    @Transactional
    public Object actualizarImagenTerreno(Usuario actor, int id, String imagenUrl) {
        Permisos.exigirAdminOSupervisor(actor);
        persistencia.bloquear(TipoCatalogo.TERRENOS, Validaciones.id(id));
        var actualizado = persistencia.actualizarImagenTerreno(id, imagenUrl);
        db.auditar(actor, "ACTUALIZAR_IMAGEN", "terrenos", id, Map.of("imagen_url", imagenUrl));
        return terrenoRepository.buscarPorId(BaseDatos.entero(actualizado, "id"));
    }

    @Transactional
    public void borrar(Usuario actor, TipoCatalogo tipo, int id) {
        Permisos.exigirAdmin(actor);
        var registro = persistencia.borrar(tipo, Validaciones.id(id));
        db.auditar(actor, "ELIMINAR", tipo.tabla(), id, registro);
    }

    private Object modelo(TipoCatalogo tipo, int id) {
        return switch(tipo) {
            case TERRENOS -> terrenoRepository.buscarPorId(id);
            case CULTIVOS -> cultivoRepository.buscarPorId(id);
            case INSUMOS -> insumoRepository.buscarPorId(id);
        };
    }

    private static void verificarUnidad(TipoCatalogo tipo, Map<String, Object> datos) {
        if (tipo == TipoCatalogo.INSUMOS && "UN".equals(datos.get("unidad")) &&
            ((BigDecimal) datos.get("stock_minimo")).stripTrailingZeros().scale() > 0) {
            throw ErrorApi.campo("stock_minimo", "El umbral por unidades debe ser un número entero.");
        }
    }

}
