package ec.agrociclo.service;

import ec.agrociclo.repository.OperacionesRepository;
import ec.agrociclo.model.*;
import ec.agrociclo.repository.*;
import ec.agrociclo.dto.MovimientoResultado;
import ec.agrociclo.security.Permisos;
import ec.agrociclo.repository.ConsultaCiclos;
import ec.agrociclo.dto.*;

import ec.agrociclo.repository.BaseDatos;
import ec.agrociclo.dto.Pagina;
import ec.agrociclo.exception.ErrorApi;
import ec.agrociclo.validation.Validaciones;
import ec.agrociclo.model.Usuario;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * OperacionesService: Implementa la lógica de negocio y las reglas del módulo.
 */
public class OperacionesService {
    private final OperacionesRepository persistencia;
    private final ActividadRepository actividadRepository;
    private final CosechaRepository cosechaRepository;
    private final MovimientoInsumoRepository movimientoInsumoRepository;
    private static final BigDecimal MAXIMO_STOCK = new BigDecimal("999999999.999");
    private final BaseDatos db;
    private final ConsultaCiclos consulta;

    public OperacionesService(BaseDatos db, ConsultaCiclos consulta, ActividadRepository actividadRepository, CosechaRepository cosechaRepository, MovimientoInsumoRepository movimientoInsumoRepository, OperacionesRepository persistencia) {
        this.persistencia=persistencia;
        this.actividadRepository=actividadRepository;
        this.cosechaRepository=cosechaRepository;
        this.movimientoInsumoRepository=movimientoInsumoRepository;
        this.db = db;
        this.consulta = consulta;
    }

    @Transactional
    public Actividad programar(Usuario actor, int cicloId, ActividadRequest datos) {
        Permisos.exigirAdminOSupervisor(actor);
        validarProgramacion(cicloId, datos);
        var fila = persistencia.programarConsultarActividades(cicloId, datos.nombre().strip(), datos.programadaPara(), datos.obligatoria(), Validaciones.nota(datos.nota()));
        db.auditar(actor, "PROGRAMAR", "actividades", BaseDatos.entero(fila, "id"),
            Map.of("ciclo_id", cicloId, "nombre", fila.get("nombre")));
        return actividadRepository.buscarPorId(BaseDatos.entero(fila, "id"));
    }

    @Transactional
    public Actividad editarActividad(Usuario actor, int cicloId, int actividadId, ActividadRequest datos) {
        Permisos.exigirAdminOSupervisor(actor);
        validarProgramacion(cicloId, datos);
        actividadPendiente(cicloId, actividadId);
        var fila = persistencia.editarActividadConsultarActividades(datos.nombre().strip(), datos.programadaPara(), datos.obligatoria(), Validaciones.nota(datos.nota()), actividadId);
        db.auditar(actor, "EDITAR", "actividades", actividadId, Map.of("ciclo_id", cicloId, "nombre", fila.get("nombre")));
        return actividadRepository.buscarPorId(BaseDatos.entero(fila, "id"));
    }

    @Transactional
    public Actividad completar(Usuario actor, int cicloId, int actividadId, CompletarRequest datos) {
        var ciclo = consulta.bloquear(cicloId);
        CiclosService.exigirAbierto(ciclo);
        actividadPendiente(cicloId, actividadId);
        Validaciones.fechaReal(datos.fecha(), BaseDatos.fecha(ciclo, "fecha_inicio"),
            "La ejecución debe estar entre el inicio del ciclo y hoy.");
        var fila = persistencia.completarConsultarActividades(datos.fecha(), datos.costo(), actividadId);
        db.auditar(actor, "COMPLETAR", "actividades", actividadId,
            Map.of("ciclo_id", cicloId, "fecha", datos.fecha(), "costo", datos.costo()));
        return actividadRepository.buscarPorId(BaseDatos.entero(fila, "id"));
    }

    @Transactional
    public void borrarActividad(Usuario actor, int cicloId, int actividadId) {
        Permisos.exigirAdmin(actor);
        CiclosService.exigirAbierto(consulta.bloquear(cicloId));
        var actividad = actividadPendiente(cicloId, actividadId);
        persistencia.borrarActividadActualizarActividades(actividadId);
        db.auditar(actor, "ELIMINAR", "actividades", actividadId, Map.of("ciclo_id", cicloId, "nombre", actividad.get("nombre")));
    }

    @Transactional
    public Cosecha cosechar(Usuario actor, int cicloId, CosechaRequest datos) {
        var ciclo = consulta.bloquear(cicloId);
        CiclosService.exigirSembrado(ciclo, "La cosecha requiere un ciclo sembrado y abierto.");
        Validaciones.fechaReal(datos.fecha(), BaseDatos.fecha(ciclo, "fecha_siembra"),
            "La cosecha debe estar entre la siembra y hoy.");
        var registro = persistencia.cosecharConsultarCosechas(cicloId, Validaciones.folio(datos.folio()), datos.fecha(), datos.kilogramos(), Validaciones.nota(datos.nota()));
        db.auditar(actor, "REGISTRAR_COSECHA", "cosechas", BaseDatos.entero(registro, "id"), registro);
        return cosechaRepository.buscarPorId(BaseDatos.entero(registro, "id"));
    }

    @Transactional
    public void retirarCosecha(Usuario actor, int cicloId, int cosechaId) {
        Permisos.exigirAdmin(actor);
        CiclosService.exigirSembrado(consulta.bloquear(cicloId), "Solo se puede retirar una cosecha de un ciclo sembrado y abierto.");
        var registro = persistencia.retirarCosechaRequerirCosechas(cicloId, Validaciones.id(cosechaId));
        db.auditar(actor, "RETIRAR_COSECHA", "cosechas", cosechaId, registro);
    }

    public Pagina movimientos(int id, Map<String, String> filtros) {
        Validaciones.parametros(filtros, Set.of("pagina", "tam"));
        int pagina = Validaciones.enteroConsulta(filtros, "pagina", 1, Integer.MAX_VALUE);
        int tam = Validaciones.enteroConsulta(filtros, "tam", 10, 50);
        persistencia.movimientosRequerirInsumos(Validaciones.id(id));
        return persistencia.movimientosPaginados(id, pagina, tam);
    }

    @Transactional
    public MovimientoResultado entrada(Usuario actor, int id, EntradaRequest datos) {
        Permisos.exigirAdminOSupervisor(actor);
        Validaciones.fechaReal(datos.fecha(), null, "La entrada debe registrarse con una fecha de hoy o anterior.");
        return mover(actor, Validaciones.id(id), null, datos);
    }

    @Transactional
    public MovimientoResultado consumo(Usuario actor, int cicloId, ConsumoRequest datos) {
        var ciclo = consulta.bloquear(cicloId);
        CiclosService.exigirSembrado(ciclo, "El consumo requiere un ciclo sembrado y abierto.");
        Validaciones.fechaReal(datos.fecha(), BaseDatos.fecha(ciclo, "fecha_siembra"),
            "La aplicación debe estar entre la siembra y hoy.");
        return mover(actor, datos.insumoId(), cicloId, datos);
    }

    private MovimientoResultado mover(Usuario actor, int id, Integer cicloId, MovimientoRequest datos) {
        // Ciclo e insumo se bloquean siempre en el mismo orden.
        var insumo = persistencia.moverRequerirInsumos(id);
        String folio = Validaciones.folio(datos.folio());
        boolean entrada = cicloId == null;
        String tipo = entrada ? "ENTRADA" : "CONSUMO";
        if (persistencia.moverConsultarMovimientosInsumo(id, tipo, folio) != null) {
            throw ErrorApi.conflicto("DUPLICADO", "Ese folio ya está registrado para el insumo y tipo de movimiento.");
        }
        if ("UN".equals(insumo.get("unidad")) && datos.cantidad().stripTrailingZeros().scale() > 0) {
            throw ErrorApi.invalido("CANTIDAD_FRACCIONARIA", "Este insumo se registra en unidades enteras.");
        }
        var cambio = entrada ? datos.cantidad() : datos.cantidad().negate();
        var saldo = BaseDatos.decimal(insumo, "stock").add(cambio);
        if (saldo.signum() < 0) throw ErrorApi.conflicto("STOCK_INSUFICIENTE", "La cantidad solicitada supera el stock disponible.");
        if (saldo.compareTo(MAXIMO_STOCK) > 0) throw ErrorApi.invalido("STOCK_MAXIMO", "La cantidad supera el stock máximo permitido.");
        var nuevoSaldo = persistencia.moverConsultarInsumos(saldo, id);
        var movimiento = persistencia.moverConsultarMovimientosInsumo2(id, tipo, cicloId, folio, datos.fecha(), datos.cantidad(), datos.costoUnitario());
        var detalle = new LinkedHashMap<String, Object>(movimiento);
        detalle.put("stock", nuevoSaldo.get("stock"));
        db.auditar(actor, tipo, "movimientos_insumo", BaseDatos.entero(movimiento, "id"), detalle);
        return new MovimientoResultado(movimientoInsumoRepository.buscarPorId(BaseDatos.entero(movimiento,"id")), saldo);
    }

    private void validarProgramacion(int id, ActividadRequest datos) {
        var ciclo = consulta.bloquear(id);
        CiclosService.exigirAbierto(ciclo);
        var fecha = Validaciones.fecha(datos.programadaPara(), "programada_para");
        if (fecha.isBefore(BaseDatos.fecha(ciclo, "fecha_inicio"))) {
            throw ErrorApi.invalido("FECHA_INVALIDA", "La actividad debe programarse desde el inicio del ciclo.");
        }
    }

    private Map<String, Object> actividadPendiente(int cicloId, int id) {
        var actividad = persistencia.actividadPendienteRequerirActividades(cicloId, Validaciones.id(id));
        if (actividad.get("completada_en") != null) {
            throw ErrorApi.conflicto("ACTIVIDAD_COMPLETADA", "La actividad ya está completada.");
        }
        return actividad;
    }
}
