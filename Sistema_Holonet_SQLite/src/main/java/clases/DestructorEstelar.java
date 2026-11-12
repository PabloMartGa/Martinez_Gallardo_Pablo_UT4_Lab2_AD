package clases;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "destructores_estelares")
@PrimaryKeyJoinColumn(name = "destructor_id", referencedColumnName = "nave_id")
public class DestructorEstelar extends Nave {

    @Column(name = "capacidad_escudos", nullable = false)
    private int capacidadEscudos;

    public DestructorEstelar() {
    }

    public DestructorEstelar(String nombre, String modelo, String estadoSistema,
                             Planeta planetaOrigen, int capacidadEscudos) {
        super(nombre, modelo, estadoSistema, planetaOrigen);
        this.capacidadEscudos = capacidadEscudos;
    }

    public int getCapacidadEscudos() {
        return capacidadEscudos;
    }

    public void setCapacidadEscudos(int capacidadEscudos) {
        this.capacidadEscudos = capacidadEscudos;
    }

    @Override
    public String toString() {
        return super.toString().replace("}", ", capacidadEscudos=" + capacidadEscudos + '}');
    }
}
