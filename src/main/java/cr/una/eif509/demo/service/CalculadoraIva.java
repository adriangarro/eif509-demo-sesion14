package cr.una.eif509.demo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Sesión 13 · Regla de negocio: el total de un pedido es la suma de sus
// líneas (el subtotal) más el 13 % de IVA. Es una función pura, sin
// dependencias, así que se verifica en el nivel más económico de la
// pirámide: una prueba unitaria de milisegundos (CalculadoraIvaTest).
public final class CalculadoraIva {

    // Tarifa general del impuesto al valor agregado en Costa Rica.
    public static final BigDecimal TARIFA = new BigDecimal("0.13");

    private CalculadoraIva() {
    }

    // El impuesto se redondea a dos decimales (céntimos de colón).
    public static BigDecimal iva(BigDecimal subtotal) {
        return subtotal.multiply(TARIFA).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal totalConIva(BigDecimal subtotal) {
        return subtotal.add(iva(subtotal)).setScale(2, RoundingMode.HALF_UP);
    }
}
