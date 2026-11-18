package ec.agrociclo.dto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import ec.agrociclo.model.MovimientoInsumo;
import java.math.BigDecimal;
/** Respuesta de movimiento con el saldo resultante, sin alterar el modelo persistido. */
public record MovimientoResultado(@JsonUnwrapped MovimientoInsumo movimiento, BigDecimal stock) {}
