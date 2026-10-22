package cr.una.eif509.demo.repository;

import cr.una.eif509.demo.PostgresContainerBase;
import cr.una.eif509.demo.model.Inventario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Sesión 14 · La prueba de la lámina 22: dos operadores, cada uno con su
// copia del mismo producto, venden 8 unidades de un inventario de 10.
//
// Sin @Transactional a propósito: si toda la prueba corriera en una sola
// transacción, las dos lecturas devolverían el mismo objeto y no habría
// conflicto que observar. Cada findById es una lectura independiente, como
// dos peticiones de dos operadores distintos.
class ConcurrenciaProductoIT extends PostgresContainerBase {

    @Autowired InventarioRepository productos;

    // La regla real: si alguien ya modificó el producto, la segunda venta no
    // puede pasar sin revisar. La primera escritura funciona y la segunda se
    // rechaza; el inventario final confirma que no se vendió de más.
    @Test
    void segundaActualizacionSimultaneaSeRechaza() {
        Long id = productos.save(new Inventario("Café de la prueba de concurrencia", 10,
                new BigDecimal("5200.00"))).getId();

        var copiaA = productos.findById(id).orElseThrow();   // operador A lee 10, versión 0
        var copiaB = productos.findById(id).orElseThrow();   // operador B lee 10, versión 0

        copiaA.reservar(8);
        productos.saveAndFlush(copiaA);   // versión 0 -> 1

        copiaB.reservar(8);               // aún tiene versión 0
        assertThatThrownBy(() -> productos.saveAndFlush(copiaB))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(productos.findById(id).orElseThrow().getDisponible())
                .isEqualTo(2);            // no se vendió de más
    }
}
