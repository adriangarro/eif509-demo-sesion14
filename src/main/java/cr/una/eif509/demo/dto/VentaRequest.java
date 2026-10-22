package cr.una.eif509.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Sesión 14 · DTO de entrada de una venta: cuántas unidades descuenta un
// operador del inventario de un producto.
public record VentaRequest(

        @Schema(description = "Unidades vendidas (1 a 1000)", example = "8")
        @NotNull(message = "Este campo es obligatorio.")
        @Positive(message = "El valor debe ser mayor que cero.")
        @Max(value = 1000, message = "El valor no puede superar 1000.")
        Integer cantidad) {
}
