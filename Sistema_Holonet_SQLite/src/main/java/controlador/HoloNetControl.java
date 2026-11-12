package controlador;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.hibernate.Transaction;

import clases.CazaLigero;
import clases.DestructorEstelar;
import clases.Nave;
import clases.Planeta;
import util.HibernateUtil;

/**
 * Operaciones de persistencia del Sistema HoloNet.
 * La jerarquía Nave/CazaLigero/DestructorEstelar se persiste con JOINED.
 */
public class HoloNetControl {

    /**
     * Operación 1: alta polimórfica. Hibernate inserta en naves y en la tabla
     * hija correspondiente según la clase concreta recibida.
     */
    public void registrarNuevaNave(Nave nave) {
        if (nave == null) {
            throw new IllegalArgumentException("La nave no puede ser null.");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            activarClavesForaneas(session);
            Transaction tx = session.beginTransaction();
            try {
                asociarPlanetaGestionado(session, nave);
                session.persist(nave);
                tx.commit();
            } catch (RuntimeException e) {
                rollbackSeguro(tx, e);
                throw e;
            }
        }
    }

    /**
     * Operación 2: sincroniza una entidad previamente desconectada.
     * merge() devuelve una instancia gestionada distinta de la recibida.
     */
    public void actualizarEstadoDesconectado(Nave naveModificada) {
        if (naveModificada == null || naveModificada.getId() == null) {
            throw new IllegalArgumentException("Se necesita una nave desconectada con ID.");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            activarClavesForaneas(session);
            Transaction tx = session.beginTransaction();
            try {
                asociarPlanetaGestionado(session, naveModificada);
                session.merge(naveModificada);
                tx.commit();
            } catch (RuntimeException e) {
                rollbackSeguro(tx, e);
                throw e;
            }
        }
    }

    /**
     * Operación 3: consulta HQL polimórfica sobre CazaLigero y carga anticipada
     * del planeta de origen para poder acceder a él tras cerrar la sesión.
     */
    public List<CazaLigero> obtenerCazasPorPotenciaMinima(int potenciaMinima) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "select c from CazaLigero c "
                    + "left join fetch c.planetaOrigen "
                    + "where c.potenciaFuego >= :potencia "
                    + "order by c.potenciaFuego desc",
                    CazaLigero.class)
                    .setParameter("potencia", potenciaMinima)
                    .getResultList();
        }
    }

    /**
     * Operación 4: baja de una nave. find(Nave.class, id) conserva el
     * comportamiento polimórfico y recupera la subclase real.
     */
    public boolean darDeBajaNave(int idNave) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            activarClavesForaneas(session);
            Transaction tx = session.beginTransaction();
            try {
                Nave nave = session.find(Nave.class, idNave);
                if (nave == null) {
                    tx.commit();
                    return false;
                }
                session.remove(nave);
                tx.commit();
                return true;
            } catch (RuntimeException e) {
                rollbackSeguro(tx, e);
                throw e;
            }
        }
    }

    /**
     * Operación 5: importa el CSV completo dentro de una sola transacción.
     * El argumento es el nombre del recurso de classpath, por ejemplo
     * "refuerzos_borde_exterior.csv".
     *
     * @return número de naves insertadas
     * @throws IllegalStateException si falla la lectura, el parseo o la BD;
     *                               en ese caso se revierte toda la importación
     */
    public int importarFlotaDesdeCSV(String rutaRecurso) {
        if (rutaRecurso == null || rutaRecurso.isBlank()) {
            throw new IllegalArgumentException("Debes indicar el recurso CSV.");
        }

        String nombreRecurso = rutaRecurso.startsWith("/")
                ? rutaRecurso.substring(1)
                : rutaRecurso;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = HoloNetControl.class.getClassLoader();
        }

        try (InputStream input = classLoader.getResourceAsStream(nombreRecurso)) {
            if (input == null) {
                throw new IllegalArgumentException(
                        "No se encontró el recurso CSV en el classpath: " + nombreRecurso);
            }

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                activarClavesForaneas(session);
                Transaction tx = session.beginTransaction();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(input, StandardCharsets.UTF_8))) {

                    String cabecera = br.readLine();
                    if (cabecera == null) {
                        throw new IllegalArgumentException("El CSV está vacío.");
                    }

                    int insertadas = 0;
                    String linea;
                    int numeroLinea = 1;

                    while ((linea = br.readLine()) != null) {
                        numeroLinea++;
                        if (linea.isBlank()) {
                            continue;
                        }

                        Nave nave = convertirLineaCSV(linea, numeroLinea, session);
                        session.persist(nave);
                        insertadas++;
                    }

                    // Fuerza aquí las comprobaciones SQL/FK, antes del commit.
                    session.flush();
                    tx.commit();
                    return insertadas;

                } catch (Exception e) {
                    rollbackSeguro(tx, e);
                    throw new IllegalStateException(
                            "Fallo en la importación CSV. Se ha revertido la transacción: "
                            + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el recurso CSV.", e);
        }
    }

    /** Recupera una nave con su planeta cargado; útil para el ciclo detached/merge. */
    public Nave obtenerNavePorId(int idNave) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "select n from Nave n left join fetch n.planetaOrigen where n.id = :id",
                    Nave.class)
                    .setParameter("id", idNave)
                    .uniqueResult();
        }
    }

    /** Devuelve todas las naves concretas; la consulta sobre Nave es polimórfica. */
    public List<Nave> obtenerTodasLasNaves() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return new ArrayList<>(session.createQuery(
                    "select n from Nave n left join fetch n.planetaOrigen order by n.id",
                    Nave.class).getResultList());
        }
    }

    private Nave convertirLineaCSV(String linea, int numeroLinea, Session session) {
        String[] datos = linea.split(";", -1);
        if (datos.length != 6) {
            throw new IllegalArgumentException("Línea " + numeroLinea
                    + ": se esperaban 6 columnas y se han encontrado " + datos.length + '.');
        }

        String tipo = datos[0].trim().toUpperCase(Locale.ROOT);
        String nombre = datos[1].trim();
        String modelo = datos[2].trim();
        String estado = datos[3].trim();
        if (nombre.isEmpty() || modelo.isEmpty() || estado.isEmpty()) {
            throw new IllegalArgumentException("Línea " + numeroLinea
                    + ": nombre, modelo y estado son obligatorios.");
        }

        final int idPlaneta;
        final int valorEspecifico;
        try {
            idPlaneta = Integer.parseInt(datos[4].trim());
            valorEspecifico = Integer.parseInt(datos[5].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Línea " + numeroLinea
                    + ": ID_PLANETA y ATRIBUTO_ESPECIFICO deben ser números enteros.", e);
        }
        if (idPlaneta <= 0 || valorEspecifico < 0) {
            throw new IllegalArgumentException("Línea " + numeroLinea
                    + ": el ID del planeta debe ser positivo y el atributo no puede ser negativo.");
        }

        // La referencia se obtiene en la misma sesión y transacción que el persist().
        Planeta planeta = session.getReference(Planeta.class, idPlaneta);

        switch (tipo) {
            case "CAZA":
                return new CazaLigero(nombre, modelo, estado, planeta, valorEspecifico);
            case "DESTRUCTOR":
                return new DestructorEstelar(nombre, modelo, estado, planeta, valorEspecifico);
            default:
                throw new IllegalArgumentException("Línea " + numeroLinea
                        + ": tipo no reconocido '" + datos[0].trim()
                        + "'. Usa CAZA o DESTRUCTOR.");
        }
    }

    /**
     * Sustituye una posible referencia detached por una referencia gestionada
     * en la sesión actual. No realiza cascade sobre Planeta.
     */
    private void asociarPlanetaGestionado(Session session, Nave nave) {
        Planeta planeta = nave.getPlanetaOrigen();
        if (planeta != null) {
            Integer idPlaneta = planeta.getId();
            if (idPlaneta == null) {
                throw new IllegalArgumentException(
                        "El planeta de origen debe existir previamente en SQLite.");
            }
            nave.setPlanetaOrigen(session.getReference(Planeta.class, idPlaneta));
        }
    }

    /** SQLite activa las claves foráneas por conexión, no de forma global. */
    private void activarClavesForaneas(Session session) {
        session.doWork(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
        });
    }

    private void rollbackSeguro(Transaction tx, Exception causa) {
        if (tx != null && tx.isActive()) {
            try {
                tx.rollback();
            } catch (RuntimeException rollbackError) {
                causa.addSuppressed(rollbackError);
            }
        }
    }
}
