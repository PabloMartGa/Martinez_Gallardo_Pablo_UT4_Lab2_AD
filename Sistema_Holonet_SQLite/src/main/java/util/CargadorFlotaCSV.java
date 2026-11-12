package util;

import controlador.HoloNetControl;

/**
 * Fachada pequeña para lanzar la importación desde recursos, siguiendo el
 * andamio que aparece en el enunciado. La transacción y el mapeo se realizan
 * en HoloNetControl.importarFlotaDesdeCSV().
 */
public final class CargadorFlotaCSV {

    private CargadorFlotaCSV() {}

    public static int importarDesdeRecursos(String nombreFichero) {
        return new HoloNetControl().importarFlotaDesdeCSV(nombreFichero);
    }
}
