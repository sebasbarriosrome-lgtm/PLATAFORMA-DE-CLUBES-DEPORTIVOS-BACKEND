package backend.dto;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String correo;
    private String password;
}