package ec.agrociclo.service;

import ec.agrociclo.repository.CiclosRepository;
import ec.agrociclo.model.*;
import ec.agrociclo.repository.*;
import ec.agrociclo.security.Permisos;
import ec.agrociclo.repository.ConsultaCiclos;
import ec.agrociclo.dto.*;

import ec.agrociclo.repository.BaseDatos;
import ec.agrociclo.exception.ErrorApi;
import ec.agrociclo.validation.Validaciones;
import ec.agrociclo.model.Usuario;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * CiclosService: Implementa la lógica de negocio y las reglas del módulo.
 */
public class CiclosService {
    private final CiclosRepository persistencia;
    private final CicloRepository cicloRepository;
    private final ActividadRepository actividadRepository;
    private final CosechaRepository cosechaRepository;
    private final BaseDatos db;
    private final ConsultaCiclos consulta;

    public CiclosService(BaseDatos db, ConsultaCiclos consulta, CicloRepository cicloRepository, ActividadRepository actividadRepository, CosechaRepository cosechaRepository, CiclosRepository persistencia) {
        this.persistencia=persistencia;
        this.cicloRepository=cicloRepository;
        this.actividadRepository=actividadRepository;
        this.cosechaRepository=cosechaRepository;
        this.db = db;
        this.consulta = consulta;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Map<String, Object> detalle(int id) {
        var ciclo = consulta.resumen(id);
        ciclo.put("actividades", actividadRepository.buscarPorCiclo(id));
        ciclo.put("cosechas", cosechaRepository.buscarPorCiclo(id));
        ciclo.put("consumos", persistencia.detalleListarMovimientosInsumo(id));
        return ciclo;
    }

    @Transactional
    public Ciclo crear(Usuario actor, PlanRequest datos) {
        Permisos.exigirAdminOSupervisor(actor);
        validarCatalogos(datos);
        var codigo = Validaciones.folio(datos.codigo());
        var nuevo = persistencia.crearConsultarCiclos(codigo, datos.terrenoId(), datos.cultivoId(), datos.areaHa(), datos.fechaInicio(), Validaciones.nota(datos.notas()));
        db.auditar(actor, "PLANIFICAR", "ciclos", BaseDatos.entero(nuevo, "id"), Map.of("codigo", codigo));
        return cicloRepository.buscarPorId(BaseDatos.entero(nuevo, "id"));
    }

    @Transactional
    public Ciclo editar(Usuario actor, int id, PlanRequest datos) {
        Permisos.exigirAdminOSupervisor(actor);
        var ciclo = consulta.bloquear(id);
        exigirPlanificado(ciclo);
        validarCatalogos(datos);
        if (persistencia.editarConsultarActividades(id, datos.fechaInicio(), datos.fechaInicio()) != null) {
            throw ErrorApi.conflicto("FECHA_EN_USO", "La nueva fecha de inicio deja una actividad fuera del ciclo.");
        }
        boolean reasignado = datos.terrenoId() != BaseDatos.entero(ciclo, "terreno_id") ||
            datos.cultivoId() != BaseDatos.entero(ciclo, "cultivo_id");
        if (reasignado && persistencia.editarConsultarActividades2(id) != null) {
            throw ErrorApi.conflicto("ASOCIACION_EN_USO", "Ya hay actividades realizadas para ese terreno y cultivo.");
        }
        var codigo = Validaciones.folio(datos.codigo());
        var editado = persistencia.editarConsultarCiclos(codigo, datos.terrenoId(), datos.cultivoId(), datos.areaHa(), datos.fechaInicio(), Validaciones.nota(datos.notas()), id);
        db.auditar(actor, "EDITAR", "ciclos", id, Map.of("codigo", codigo));
        return cicloRepository.buscarPorId(BaseDatos.entero(editado, "id"));
    }

    @Transactional
    public Ciclo sembrar(Usuario actor, int id, LocalDate fecha) {
        Permisos.exigirAdminOSupervisor(actor);
        var ciclo = consulta.bloquear(id);
        exigirPlanificado(ciclo);
        Validaciones.fechaReal(fecha, BaseDatos.fecha(ciclo, "fecha_inicio"), "La siembra debe estar entre el inicio del ciclo y hoy.");
        var sembrado = persistencia.sembrarConsultarCiclos(fecha, fecha, id);
        db.auditar(actor, "SEMBRAR", "ciclos", id, Map.of("fecha", fecha));
        return cicloRepository.buscarPorId(BaseDatos.entero(sembrado, "id"));
    }

    @Transactional
    public Ciclo cerrar(Usuario actor, int id, LocalDate fecha) {
        Permisos.exigirAdminOSupervisor(actor);
        var ciclo = consulta.bloquear(id);
        exigirSembrado(ciclo, "Solo se pueden cerrar ciclos sembrados.");
        Validaciones.fechaReal(fecha, BaseDatos.fecha(ciclo, "fecha_siembra"), "El cierre debe estar entre la siembra y hoy.");
        if (persistencia.cerrarConsultarActividades(id) != null) {
            throw ErrorApi.conflicto("ACTIVIDADES_PENDIENTES", "Completa las actividades obligatorias antes de cerrar.");
        }
        var registro = persistencia.cerrarConsultarActividades2(id, id, id);
        var ultima = BaseDatos.fecha(registro, "fecha");
        if (ultima != null && fecha.isBefore(ultima)) {
            throw ErrorApi.invalido("FECHA_CIERRE", "El cierre debe ser posterior o igual al último registro del ciclo.");
        }
        var cerrado = persistencia.cerrarConsultarCiclos(fecha, id);
        db.auditar(actor, "CERRAR", "ciclos", id, Map.of("fecha", fecha));
        return cicloRepository.buscarPorId(BaseDatos.entero(cerrado, "id"));
    }

    @Transactional
    public void eliminar(Usuario actor, int id) {
        Permisos.exigirAdmin(actor);
        var ciclo = consulta.bloquear(id);
        exigirPlanificado(ciclo);
        persistencia.eliminarActualizarCiclos(id);
        db.auditar(actor, "ELIMINAR", "ciclos", id, Map.of("codigo", ciclo.get("codigo")));
    }

    private void validarCatalogos(PlanRequest datos) {
        Validaciones.fecha(datos.fechaInicio(), "fecha_inicio");
        var terreno = persistencia.validarCatalogosRequerirTerrenos(datos.terrenoId());
        persistencia.validarCatalogosRequerirCultivos(datos.cultivoId());
        if (BaseDatos.decimal(terreno, "area_ha").compareTo(datos.areaHa()) < 0) {
            throw ErrorApi.invalido("AREA_EXCEDIDA", "El área del ciclo supera la superficie del terreno.");
        }
    }

    public static void exigirSembrado(Map<String, Object> ciclo, String mensaje) {
        if (!"SEMBRADO".equals(ciclo.get("estado"))) throw ErrorApi.conflicto("CICLO_NO_SEMBRADO", mensaje);
    }

    public static void exigirAbierto(Map<String, Object> ciclo) {
        if ("CERRADO".equals(ciclo.get("estado"))) throw ErrorApi.conflicto("CICLO_CERRADO", "El ciclo está cerrado.");
    }

    private static void exigirPlanificado(Map<String, Object> ciclo) {
        if (!"PLANIFICADO".equals(ciclo.get("estado"))) {
            throw ErrorApi.conflicto("CICLO_NO_PLANIFICADO", "Esta acción está disponible en ciclos planificados.");
        }
    }
}
