package cr.una.eif509.demo.repository;

import cr.una.eif509.demo.PostgresContainerBase;
import cr.una.eif509.demo.model.Inventario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// Sesión 14 · La prueba de la lámina 22: dos operadores, cada uno con su
// copia del mismo producto, venden 8 unidades de un inventario de 10.
//
// Sin @Transactional a propósito: si toda la prueba corriera en una sola
// transacción, las dos lecturas devolverían el mismo objeto y no habría
// conflicto que observar. Cada findById es una lectura independiente, como
// dos peticiones de dos operadores distintos.
class ConcurrenciaProductoIT extends PostgresContainerBase {

    @Autowired InventarioRepository productos;

    // Versión inicial: verifica que el inventario final sea 2... y lo es.
    // Pero se vendieron 16 unidades. La prueba pasa y el sistema está mal.
    @Test
    void segundaActualizacionSimultaneaSeRechaza() {
        Long id = productos.save(new Inventario("Café de la prueba de concurrencia", 10,
                new BigDecimal("5200.00"))).getId();

        var copiaA = productos.findById(id).orElseThrow();   // operador A lee 10
        var copiaB = productos.findById(id).orElseThrow();   // operador B lee 10

        copiaA.reservar(8);
        productos.saveAndFlush(copiaA);   // A guarda: quedan 2

        copiaB.reservar(8);               // B todavía cree que hay 10
        productos.saveAndFlush(copiaB);   // B guarda: quedan 2

        assertThat(productos.findById(id).orElseThrow().getDisponible())
                .isEqualTo(2);
    }
}
