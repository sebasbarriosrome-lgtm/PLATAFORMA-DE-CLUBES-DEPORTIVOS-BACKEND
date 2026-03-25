package backend.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String correo;

    @JsonIgnore
    private String password;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL)
    @JsonIgnore
    private Perfil perfil;
}