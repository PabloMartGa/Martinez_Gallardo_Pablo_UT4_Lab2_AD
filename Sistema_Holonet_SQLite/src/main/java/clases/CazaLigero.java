package clases;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "cazas_ligeros")
@PrimaryKeyJoinColumn(name = "caza_id", referencedColumnName = "nave_id")
public class CazaLigero extends Nave {

    @Column(name = "potencia_fuego", nullable = false)
    private int potenciaFuego;

    public CazaLigero() {
    }

    public CazaLigero(String nombre, String modelo, String estadoSistema,
                      Planeta planetaOrigen, int potenciaFuego) {
        super(nombre, modelo, estadoSistema, planetaOrigen);
        this.potenciaFuego = potenciaFuego;
    }

    public int getPotenciaFuego() {
        return potenciaFuego;
    }

    public void setPotenciaFuego(int potenciaFuego) {
        this.potenciaFuego = potenciaFuego;
    }

    @Override
    public String toString() {
        return super.toString().replace("}", ", potenciaFuego=" + potenciaFuego + '}');
    }
}
