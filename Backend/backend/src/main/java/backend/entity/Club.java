package backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "club")
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deleted_at")
    private java.sql.Timestamp deletedAt;

    private String nombre;
    private String slug;
    private String ciudad;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String bannerUrl;

    private String colorPrimario;
    private String colorSecundario;

    private String contacto;

    private String estado = "activo";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Club() {
    }

    public Club(String nombre, String ciudad) {
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.createdAt = LocalDateTime.now();
    }

    // Getters

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSlug() {
        return slug;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public String getColorPrimario() {
        return colorPrimario;
    }

    public String getColorSecundario() {
        return colorSecundario;
    }

    public String getContacto() {
        return contacto;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public void setColorPrimario(String colorPrimario) {
        this.colorPrimario = colorPrimario;
    }

    public void setColorSecundario(String colorSecundario) {
        this.colorSecundario = colorSecundario;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}