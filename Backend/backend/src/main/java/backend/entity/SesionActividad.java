package backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sesion_actividad")
public class SesionActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sesion_id")
    private Long sesionId;

    @Column(name = "actividad_id")
    private Long actividadId;

    private Integer orden;

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSesionId() { return sesionId; }
    public void setSesionId(Long sesionId) { this.sesionId = sesionId; }

    public Long getActividadId() { return actividadId; }
    public void setActividadId(Long actividadId) { this.actividadId = actividadId; }

    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }

    public Integer getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(Integer duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}