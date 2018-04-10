package pe.sulca.eventos;

/**
 * Created by William_ST on 01/04/18.
 */

public class EventFirestore {

    private String ciudad;
    private String evento;
    private String fecha;
    private String imagen;

    public EventFirestore() {}

    public EventFirestore(String ciudad, String evento, String fecha, String imagen) {
        this.ciudad = ciudad;
        this.evento = evento;
        this.fecha = fecha;
        this.imagen = imagen;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getEvento() {
        return evento;
    }

    public void setEvento(String evento) {
        this.evento = evento;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

}
