-- Actualización de esquema para categorías y grupos
ALTER TABLE categoria ADD COLUMN descripcion TEXT NULL;
ALTER TABLE grupo_deportivo ADD COLUMN descripcion TEXT NULL;

DELIMITER $$

CREATE PROCEDURE sp_get_horarios_by_club(
    IN p_club_id BIGINT
)
BEGIN
    SELECT
        he.id,
        he.dia_semana,
        TIME_FORMAT(he.hora_inicio, '%H:%i') AS hora_inicio,
        TIME_FORMAT(he.hora_fin, '%H:%i') AS hora_fin,
        he.descripcion,
        he.ubicacion,
        he.activo
    FROM horario_entrenamiento he
    JOIN grupo_deportivo g ON g.id = he.grupo_id
    WHERE g.club_id = p_club_id
      AND he.activo = TRUE
    ORDER BY FIELD(
        he.dia_semana,
        'lunes','martes','miercoles','jueves','viernes','sabado','domingo'
    ), he.hora_inicio;
END $$

CREATE PROCEDURE sp_create_categoria(
    IN p_club_id BIGINT,
    IN p_nombre VARCHAR(50),
    IN p_descripcion TEXT
)
BEGIN
    INSERT INTO categoria(club_id, nombre, descripcion, created_at)
    VALUES(p_club_id, p_nombre, p_descripcion, CURRENT_TIMESTAMP);
    SELECT LAST_INSERT_ID() AS categoria_id;
END $$

CREATE PROCEDURE sp_update_categoria(
    IN p_categoria_id BIGINT,
    IN p_nombre VARCHAR(50),
    IN p_descripcion TEXT
)
BEGIN
    UPDATE categoria
    SET nombre = p_nombre,
        descripcion = p_descripcion
    WHERE id = p_categoria_id;
END $$

CREATE PROCEDURE sp_delete_categoria(
    IN p_categoria_id BIGINT
)
BEGIN
    UPDATE categoria
    SET deleted_at = CURRENT_TIMESTAMP
    WHERE id = p_categoria_id;
END $$

CREATE PROCEDURE sp_get_categorias_by_club(
    IN p_club_id BIGINT,
    IN p_search VARCHAR(50)
)
BEGIN
    SELECT id, club_id, nombre, descripcion, created_at
    FROM categoria
    WHERE club_id = p_club_id
      AND deleted_at IS NULL
      AND (p_search = '' OR LOWER(nombre) LIKE LOWER(CONCAT('%', p_search, '%')))
    ORDER BY nombre;
END $$

CREATE PROCEDURE sp_get_categoria_by_id(
    IN p_categoria_id BIGINT
)
BEGIN
    SELECT id, club_id, nombre, descripcion, created_at
    FROM categoria
    WHERE id = p_categoria_id
      AND deleted_at IS NULL
    LIMIT 1;
END $$

CREATE PROCEDURE sp_create_grupo_deportivo(
    IN p_club_id BIGINT,
    IN p_nombre VARCHAR(50),
    IN p_descripcion TEXT
)
BEGIN
    INSERT INTO grupo_deportivo(club_id, nombre, descripcion, created_at)
    VALUES(p_club_id, p_nombre, p_descripcion, CURRENT_TIMESTAMP);
    SELECT LAST_INSERT_ID() AS grupo_id;
END $$

CREATE PROCEDURE sp_update_grupo_deportivo(
    IN p_grupo_id BIGINT,
    IN p_nombre VARCHAR(50),
    IN p_descripcion TEXT
)
BEGIN
    UPDATE grupo_deportivo
    SET nombre = p_nombre,
        descripcion = p_descripcion
    WHERE id = p_grupo_id;
END $$

CREATE PROCEDURE sp_delete_grupo_deportivo(
    IN p_grupo_id BIGINT
)
BEGIN
    UPDATE grupo_deportivo
    SET deleted_at = CURRENT_TIMESTAMP
    WHERE id = p_grupo_id;
END $$

CREATE PROCEDURE sp_get_grupos_by_club(
    IN p_club_id BIGINT,
    IN p_search VARCHAR(50)
)
BEGIN
    SELECT id, club_id, nombre, descripcion, created_at
    FROM grupo_deportivo
    WHERE club_id = p_club_id
      AND deleted_at IS NULL
      AND (p_search = '' OR LOWER(nombre) LIKE LOWER(CONCAT('%', p_search, '%')))
    ORDER BY nombre;
END $$

CREATE PROCEDURE sp_get_grupo_by_id(
    IN p_grupo_id BIGINT
)
BEGIN
    SELECT id, club_id, nombre, descripcion, created_at
    FROM grupo_deportivo
    WHERE id = p_grupo_id
      AND deleted_at IS NULL
    LIMIT 1;
END $$

CREATE PROCEDURE sp_clear_entrenadores_categoria(
    IN p_categoria_id BIGINT
)
BEGIN
    DELETE FROM entrenador_categoria WHERE categoria_id = p_categoria_id;
END $$

CREATE PROCEDURE sp_insert_entrenador_categoria(
    IN p_categoria_id BIGINT,
    IN p_entrenador_id BIGINT
)
BEGIN
    INSERT INTO entrenador_categoria(categoria_id, entrenador_id)
    VALUES(p_categoria_id, p_entrenador_id);
END $$

CREATE PROCEDURE sp_clear_entrenadores_grupo(
    IN p_grupo_id BIGINT
)
BEGIN
    DELETE FROM grupo_entrenador WHERE grupo_id = p_grupo_id;
END $$

CREATE PROCEDURE sp_insert_grupo_entrenador(
    IN p_grupo_id BIGINT,
    IN p_entrenador_id BIGINT
)
BEGIN
    INSERT INTO grupo_entrenador(grupo_id, entrenador_id)
    VALUES(p_grupo_id, p_entrenador_id);
END $$

DELIMITER ;
