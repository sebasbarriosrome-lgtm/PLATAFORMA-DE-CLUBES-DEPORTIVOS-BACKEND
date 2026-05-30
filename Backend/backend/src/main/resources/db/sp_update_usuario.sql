-- Stored Procedure para actualizar el perfil del usuario
DROP PROCEDURE IF EXISTS sp_update_usuario;

DELIMITER //

CREATE PROCEDURE sp_update_usuario(
    IN p_email_actual VARCHAR(255),
    IN p_nombre VARCHAR(100),
    IN p_apellido VARCHAR(100),
    IN p_email VARCHAR(255),
    IN p_telefono VARCHAR(20),
    IN p_birthDate DATE,
    IN p_photoUrl VARCHAR(500)
)
BEGIN
    DECLARE usuario_id_var BIGINT;

    -- Obtener el ID del usuario actual
    SELECT id INTO usuario_id_var FROM usuario WHERE email = p_email_actual;

    IF usuario_id_var IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Usuario no encontrado';
    END IF;

    -- Validar que el email no esté en uso por otro usuario (si se cambió)
    IF p_email IS NOT NULL AND p_email != p_email_actual THEN
        IF EXISTS(SELECT 1 FROM usuario WHERE email = p_email AND id != usuario_id_var) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El correo ya está registrado';
        END IF;
    END IF;

    -- Actualizar datos del usuario
    UPDATE usuario SET
        nombre = COALESCE(p_nombre, nombre),
        apellido = COALESCE(p_apellido, apellido),
        email = COALESCE(p_email, email),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = usuario_id_var;

    -- Crear o actualizar el perfil del usuario
    IF EXISTS(SELECT 1 FROM perfil_usuario WHERE usuario_id = usuario_id_var) THEN
        -- Actualizar perfil existente
        UPDATE perfil_usuario SET
            telefono = COALESCE(p_telefono, telefono),
            fecha_nacimiento = COALESCE(p_birthDate, fecha_nacimiento),
            foto_url = COALESCE(p_photoUrl, foto_url),
            updated_at = CURRENT_TIMESTAMP
        WHERE usuario_id = usuario_id_var;
    ELSE
        -- Crear nuevo perfil
        INSERT INTO perfil_usuario (usuario_id, telefono, fecha_nacimiento, foto_url, created_at, updated_at)
        VALUES (usuario_id_var, p_telefono, p_birthDate, p_photoUrl, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
    END IF;
END //

DELIMITER ;
