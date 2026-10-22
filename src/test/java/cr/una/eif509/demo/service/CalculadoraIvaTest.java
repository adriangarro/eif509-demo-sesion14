package cr.una.eif509.demo.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// Sesión 13 · Una buena prueba unitaria: estructura AAA, un nombre que
// describe la regla y una aserción sobre el resultado (no sobre cómo se
// calcula). Es la versión corregida de la prueba 1 de la actividad: si
// alguien cambia el 13 % por otro valor, estas pruebas fallan.
class CalculadoraIvaTest {

    @Test
    void totalConIva_sumaElTrecePorCientoAlSubtotal() {
        // Arrange: dos unidades de 1000 colones
        var subtotal = new BigDecimal("2000.00");

        // Act
        var total = CalculadoraIva.totalConIva(subtotal);

        // Assert
        assertThat(total).isEqualByComparingTo("2260.00");
    }

    @Test
    void iva_seRedondeaADosDecimales() {
        var subtotal = new BigDecimal("2500.75");

        var iva = CalculadoraIva.iva(subtotal);

        // 2500.75 × 0.13 = 325.0975, que se redondea a 325.10
        assertThat(iva).isEqualByComparingTo("325.10");
        assertThat(CalculadoraIva.totalConIva(subtotal)).isEqualByComparingTo("2825.85");
    }

    @Test
    void iva_deUnSubtotalEnCeroEsCero() {
        var total = CalculadoraIva.totalConIva(BigDecimal.ZERO);

        assertThat(total).isEqualByComparingTo("0.00");
    }
}
