package backend.entity;

import jakarta.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;

@Entity
@Table(name = "deportista")
public class Deportista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_club_id")
    private Long usuarioClubId;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(name = "grupo_id")
    private Long grupoId;

    @Column(name = "fecha_nacimiento")
    private Date fechaNacimiento;

    private Double peso;

    private Long estatura;

    @Column(name = "acudiente_id")
    private Long acudienteId;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "deleted_at")
    private Timestamp deletedAt;

    // getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioClubId() {
        return usuarioClubId;
    }

    public void setUsuarioClubId(Long usuarioClubId) {
        this.usuarioClubId = usuarioClubId;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Long getGrupoId() {
        return grupoId;
    }

    public void setGrupoId(Long grupoId) {
        this.grupoId = grupoId;
    }

    public Date getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(Date fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public Double getPeso() {
        return peso;
    }

    public void setPeso(Double peso) {
        this.peso = peso;
    }

    public Long getEstatura() {
        return estatura;
    }

    public void setEstatura(Long estatura) {
        this.estatura = estatura;
    }

    public Long getAcudienteId() {
        return acudienteId;
    }

    public void setAcudienteId(Long acudienteId) {
        this.acudienteId = acudienteId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Timestamp deletedAt) {
        this.deletedAt = deletedAt;
    }

}