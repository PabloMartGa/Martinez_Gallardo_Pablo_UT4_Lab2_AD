package com.mycompany.sistema_holonet_sqlite;

import java.util.List;
import controlador.HoloNetControl;
import clases.CazaLigero;
import clases.Nave;
import util.HibernateUtil;

/**
 * Punto de entrada sencillo para probar el laboratorio desde Maven.
 * Comandos: listar | cazas [potenciaMinima] | importar
 */
public class Sistema_Holonet_SQLite {

    public static void main(String[] args) {
        HoloNetControl control = new HoloNetControl();
        String comando = args.length == 0 ? "listar" : args[0].toLowerCase();

        try {
            switch (comando) {
                case "importar": {
                    int total = control.importarFlotaDesdeCSV("refuerzos_borde_exterior.csv");
                    System.out.println("[HoloNet] Naves importadas correctamente: " + total);
                    break;
                }
                case "cazas": {
                    int potencia = args.length > 1 ? Integer.parseInt(args[1]) : 80;
                    List<CazaLigero> cazas = control.obtenerCazasPorPotenciaMinima(potencia);
                    System.out.println("Cazas con potencia >= " + potencia + ':');
                    cazas.forEach(System.out::println);
                    break;
                }
                case "listar":
                    List<Nave> naves = control.obtenerTodasLasNaves();
                    System.out.println("Flota registrada (" + naves.size() + " naves):");
                    naves.forEach(System.out::println);
                    break;
                default:
                    System.out.println("Uso: mvn exec:java -Dexec.args=\"listar\"");
                    System.out.println("  o: mvn exec:java -Dexec.args=\"cazas 80\"");
                    System.out.println("  o: mvn exec:java -Dexec.args=\"importar\"");
            }
        } finally {
            HibernateUtil.shutdown();
        }
    }
}
