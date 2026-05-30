-- Stored Procedure para actualizar información de personalización del club
DROP PROCEDURE IF EXISTS sp_update_club;

DELIMITER //

CREATE PROCEDURE sp_update_club(
    IN p_id BIGINT,
    IN p_descripcion VARCHAR(1000),
    IN p_logo_url VARCHAR(500),
    IN p_banner_url VARCHAR(500),
    IN p_color_primario VARCHAR(20),
    IN p_color_secundario VARCHAR(20)
)
BEGIN
    UPDATE club SET
        descripcion = IFNULL(p_descripcion, descripcion),
        logo_url = IFNULL(p_logo_url, logo_url),
        banner_url = IFNULL(p_banner_url, banner_url),
        color_primario = IFNULL(p_color_primario, color_primario),
        color_secundario = IFNULL(p_color_secundario, color_secundario),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_id AND deleted_at IS NULL;
END //

DELIMITER ;
