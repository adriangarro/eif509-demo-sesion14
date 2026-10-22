package cr.una.eif509.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Sesión 14 · Solo para la demostración: simula el tiempo que pasa entre leer
// un dato y guardarlo (en una aplicación real, el que el usuario pasa frente
// a la pantalla). Con ella, dos peticiones simultáneas leen el mismo
// inventario antes de que cualquiera de las dos guarde.
//
// Vale 0 salvo en el perfil dev (application-dev.yml), así que no afecta las
// pruebas ni producción.
@Component
public class PausaEntreLeerYGuardar {

    private final Duration duracion;

    public PausaEntreLeerYGuardar(@Value("${demo.pausa-entre-leer-y-guardar:0s}") Duration duracion) {
        this.duracion = duracion;
    }

    public void esperar() {
        if (duracion.isZero()) {
            return;
        }
        try {
            Thread.sleep(duracion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
