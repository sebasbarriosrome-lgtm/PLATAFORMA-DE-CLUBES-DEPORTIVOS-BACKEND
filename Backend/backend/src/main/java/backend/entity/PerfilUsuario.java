package backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
@Table(name = "perfil_usuario")
public class PerfilUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // RELACIÓN
    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "foto_url")
    private String fotoUrl;

    // GETTERS Y SETTERS

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }
}