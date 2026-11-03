-- =============================================================================
-- V25__rg005_inmuebles_maestros_y_plan_trabajo.sql
-- Módulo de Autoevaluación e Infraestructura
-- Padrón Maestro de Inmuebles UNdeC, Espacios Agrupados (RG-005) y Vínculo con Plan de Trabajo
-- =============================================================================

-- ----------------------------------------------------------------------------
-- 1. Ampliación de columnas en tabla inmueble
-- ----------------------------------------------------------------------------
ALTER TABLE inmueble
    ADD COLUMN IF NOT EXISTS numero_orden             INT,
    ADD COLUMN IF NOT EXISTS calle                    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS numero                   VARCHAR(50),
    ADD COLUMN IF NOT EXISTS anio_construccion        INT,
    ADD COLUMN IF NOT EXISTS anio_inicio_institucion  INT,
    ADD COLUMN IF NOT EXISTS situacion                VARCHAR(100) DEFAULT 'Propiedad',
    ADD COLUMN IF NOT EXISTS vincula_todas_carreras   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS es_maestro               BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS proceso_id               UUID,
    ADD COLUMN IF NOT EXISTS estado_coneau            VARCHAR(50) NOT NULL DEFAULT 'CARGADO';

ALTER TABLE inmueble
    DROP CONSTRAINT IF EXISTS fk_inmueble_proceso;

CREATE INDEX IF NOT EXISTS ix_inmueble_proceso_id ON inmueble (proceso_id);
CREATE INDEX IF NOT EXISTS ix_inmueble_es_maestro ON inmueble (es_maestro);

-- ----------------------------------------------------------------------------
-- 2. Ampliación de columnas en tabla espacio_fisico
-- ----------------------------------------------------------------------------
ALTER TABLE espacio_fisico
    ADD COLUMN IF NOT EXISTS descripcion_infraestructura TEXT,
    ADD COLUMN IF NOT EXISTS cantidad                    INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS capacidad_personas_sentadas INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS estado_coneau               VARCHAR(50) NOT NULL DEFAULT 'CARGADO';

-- ----------------------------------------------------------------------------
-- 3. Inserción de Localidad 'La Puntilla' si no existe
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_prov_id BIGINT;
    v_depto_id BIGINT;
BEGIN
    SELECT id INTO v_prov_id FROM geografia_provincia WHERE codigo = 'LR' LIMIT 1;
    IF v_prov_id IS NOT NULL THEN
        SELECT id INTO v_depto_id FROM geografia_departamento WHERE provincia_id = v_prov_id AND nombre ILIKE '%Chilecito%' LIMIT 1;
        IF v_depto_id IS NOT NULL THEN
            IF NOT EXISTS (SELECT 1 FROM geografia_localidad WHERE departamento_id = v_depto_id AND nombre ILIKE 'La Puntilla') THEN
                INSERT INTO geografia_localidad (departamento_id, nombre, codigo_postal)
                VALUES (v_depto_id, 'La Puntilla', '5360');
            END IF;
        END IF;
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 4. Ampliación de tipos de espacio físico en catálogo
-- ----------------------------------------------------------------------------
INSERT INTO tipo_espacio_fisico (codigo, denominacion) VALUES
    ('AUDITORIO', 'Auditorio / Ámbito de reunión'),
    ('AULA', 'Aula Común'),
    ('AULA_TALLER', 'Aula - Taller'),
    ('GABINETE_ENFERMERIA', 'Gabinete de Enfermería'),
    ('SALA_AUDIENCIA', 'Sala de Audiencia y Cámara Gesell'),
    ('SALA_MULTIPROPOSITO', 'Sala Multipropósito'),
    ('LAB_QUIMICA', 'Laboratorio Químico'),
    ('LAB_FISICA', 'Laboratorio Físico'),
    ('LAB_MICROSCOPIA', 'Laboratorio de Microscopía'),
    ('LAB_MEDIOS', 'Laboratorio de Medios y Comunicaciones'),
    ('LAB_PROGRAMACION', 'Laboratorio de Programación y Base de Datos'),
    ('LAB_SISTEMAS_OPERATIVOS', 'Laboratorio de Sistemas Operativos y Software Libre'),
    ('LAB_PROGRESAR', 'Laboratorio PROGRESAR'),
    ('LAB_ALTA_COMPLEJIDAD', 'Laboratorios de Alta Complejidad (LAC)'),
    ('LAB_COVID', 'Laboratorio COVID'),
    ('LAB_ESTUDIOS_REGIONALES', 'Laboratorios de Estudios Regionales (LER)'),
    ('LAB_INCHI', 'Laboratorios INCHI'),
    ('LAB_MECATRONICA_REDES', 'Laboratorio de Mecatrónica y Redes'),
    ('LAB_ENERGIAS_RENOVABLES', 'Laboratorio de Energías Renovables'),
    ('OFICINA_INVESTIGACION', 'Oficinas de Investigación / Institutos'),
    ('OFICINA_DOCENTE', 'Oficinas de Docentes e Investigadores'),
    ('OFICINA_GESTION', 'Oficina de Gestión Académica / Bedelía / Despacho'),
    ('AMBITO_REUNION', 'Ámbito de Reuniones'),
    ('ESTUDIO_RADIO_TV', 'Estudio de Radio y TV / Audiovisual'),
    ('SALA_VIDEOCONFERENCIA', 'Sala de Videoconferencias SIED'),
    ('MESA_ENTRADAS', 'Mesa de Entradas / Atención'),
    ('CAMPO_EXPERIMENTAL', 'Campo Experimental y Productivo'),
    ('GALPON_CRIA', 'Galpón Productivo / Cría')
ON CONFLICT (codigo) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 5. Carga de los 12 Inmuebles Maestros Oficiales UNdeC (Padrón Permanente)
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_inst_id UUID;
    v_prov_id BIGINT;
    v_depto_id BIGINT;
    v_loc_sarmientos_id BIGINT;
    v_loc_chilecito_id BIGINT;
    v_loc_malligasta_id BIGINT;
    v_loc_tilimuqui_id BIGINT;
    v_loc_puntilla_id BIGINT;
    v_loc_sanmiguel_id BIGINT;

    v_carrera_agro_id UUID;
    v_inm1_id UUID := 'b1000000-0000-0000-0000-000000000001';
    v_inm2_id UUID := 'b1000000-0000-0000-0000-000000000002';
    v_inm3_id UUID := 'b1000000-0000-0000-0000-000000000003';
    v_inm4_id UUID := 'b1000000-0000-0000-0000-000000000004';
    v_inm5_id UUID := 'b1000000-0000-0000-0000-000000000005';
    v_inm6_id UUID := 'b1000000-0000-0000-0000-000000000006';
    v_inm7_id UUID := 'b1000000-0000-0000-0000-000000000007';
    v_inm8_id UUID := 'b1000000-0000-0000-0000-000000000008';
    v_inm9_id UUID := 'b1000000-0000-0000-0000-000000000009';
    v_inm10_id UUID := 'b1000000-0000-0000-0000-000000000010';
    v_inm11_id UUID := 'b1000000-0000-0000-0000-000000000011';
    v_inm12_id UUID := 'b1000000-0000-0000-0000-000000000012';

    v_tipo_auditorio UUID;
    v_tipo_aula UUID;
    v_tipo_gabinete UUID;
    v_tipo_camara UUID;
    v_tipo_lab_inf UUID;
    v_tipo_multiproposito UUID;
    v_tipo_biblio UUID;
    v_tipo_lab_quimica UUID;
    v_tipo_lab_fisica UUID;
    v_tipo_lab_micro UUID;
    v_tipo_lab_medios UUID;
    v_tipo_lab_prog UUID;
    v_tipo_lab_so UUID;
    v_tipo_lab_progreso UUID;
    v_tipo_lab_lac UUID;
    v_tipo_lab_covid UUID;
    v_tipo_lab_ler UUID;
    v_tipo_lab_inchi UUID;
    v_tipo_lab_meca UUID;
    v_tipo_lab_energias UUID;
    v_tipo_oficina UUID;
    v_tipo_reuniones UUID;
    v_tipo_estudio_tv UUID;
    v_tipo_sied UUID;
    v_tipo_mesa UUID;
    v_tipo_campo UUID;
    v_tipo_galpon UUID;
BEGIN
    SELECT id INTO v_inst_id FROM institucion LIMIT 1;
    SELECT id INTO v_prov_id FROM geografia_provincia WHERE codigo = 'LR' LIMIT 1;
    SELECT id INTO v_depto_id FROM geografia_departamento WHERE provincia_id = v_prov_id AND nombre ILIKE '%Chilecito%' LIMIT 1;

    SELECT id INTO v_loc_sarmientos_id FROM geografia_localidad WHERE departamento_id = v_depto_id AND nombre ILIKE '%Sarmientos%' LIMIT 1;
    SELECT id INTO v_loc_chilecito_id FROM geografia_localidad WHERE departamento_id = v_depto_id AND nombre ILIKE 'Chilecito' LIMIT 1;
    SELECT id INTO v_loc_malligasta_id FROM geografia_localidad WHERE departamento_id = v_depto_id AND nombre ILIKE '%Malligasta%' LIMIT 1;
    SELECT id INTO v_loc_tilimuqui_id FROM geografia_localidad WHERE departamento_id = v_depto_id AND nombre ILIKE '%Tilimuqui%' LIMIT 1;
    SELECT id INTO v_loc_puntilla_id FROM geografia_localidad WHERE departamento_id = v_depto_id AND nombre ILIKE '%Puntilla%' LIMIT 1;
    SELECT id INTO v_loc_sanmiguel_id FROM geografia_localidad WHERE departamento_id = v_depto_id AND nombre ILIKE '%San Miguel%' LIMIT 1;

    -- Defaults de seguridad para localidades
    IF v_loc_sarmientos_id IS NULL THEN v_loc_sarmientos_id := v_loc_chilecito_id; END IF;
    IF v_loc_malligasta_id IS NULL THEN v_loc_malligasta_id := v_loc_chilecito_id; END IF;
    IF v_loc_tilimuqui_id IS NULL THEN v_loc_tilimuqui_id := v_loc_chilecito_id; END IF;
    IF v_loc_puntilla_id IS NULL THEN v_loc_puntilla_id := v_loc_chilecito_id; END IF;
    IF v_loc_sanmiguel_id IS NULL THEN v_loc_sanmiguel_id := v_loc_chilecito_id; END IF;

    SELECT id INTO v_carrera_agro_id FROM carrera WHERE sigla ILIKE '%AGRO%' OR nombre ILIKE '%Agron%' LIMIT 1;

    -- Obtener tipos
    SELECT id INTO v_tipo_auditorio FROM tipo_espacio_fisico WHERE codigo = 'AUDITORIO' LIMIT 1;
    SELECT id INTO v_tipo_aula FROM tipo_espacio_fisico WHERE codigo = 'AULA' OR codigo = 'AULA_COMUN' LIMIT 1;
    SELECT id INTO v_tipo_gabinete FROM tipo_espacio_fisico WHERE codigo = 'GABINETE_ENFERMERIA' LIMIT 1;
    SELECT id INTO v_tipo_camara FROM tipo_espacio_fisico WHERE codigo = 'SALA_AUDIENCIA' LIMIT 1;
    SELECT id INTO v_tipo_lab_inf FROM tipo_espacio_fisico WHERE codigo = 'LAB_INFORMATICA' LIMIT 1;
    SELECT id INTO v_tipo_multiproposito FROM tipo_espacio_fisico WHERE codigo = 'SALA_MULTIPROPOSITO' LIMIT 1;
    SELECT id INTO v_tipo_biblio FROM tipo_espacio_fisico WHERE codigo = 'CENTRO_DOCUMENTACION' LIMIT 1;
    SELECT id INTO v_tipo_lab_quimica FROM tipo_espacio_fisico WHERE codigo = 'LAB_QUIMICA' LIMIT 1;
    SELECT id INTO v_tipo_lab_fisica FROM tipo_espacio_fisico WHERE codigo = 'LAB_FISICA' LIMIT 1;
    SELECT id INTO v_tipo_lab_micro FROM tipo_espacio_fisico WHERE codigo = 'LAB_MICROSCOPIA' LIMIT 1;
    SELECT id INTO v_tipo_lab_medios FROM tipo_espacio_fisico WHERE codigo = 'LAB_MEDIOS' LIMIT 1;
    SELECT id INTO v_tipo_lab_prog FROM tipo_espacio_fisico WHERE codigo = 'LAB_PROGRAMACION' LIMIT 1;
    SELECT id INTO v_tipo_lab_so FROM tipo_espacio_fisico WHERE codigo = 'LAB_SISTEMAS_OPERATIVOS' LIMIT 1;
    SELECT id INTO v_tipo_lab_progreso FROM tipo_espacio_fisico WHERE codigo = 'LAB_PROGRESAR' LIMIT 1;
    SELECT id INTO v_tipo_lab_lac FROM tipo_espacio_fisico WHERE codigo = 'LAB_ALTA_COMPLEJIDAD' LIMIT 1;
    SELECT id INTO v_tipo_lab_covid FROM tipo_espacio_fisico WHERE codigo = 'LAB_COVID' LIMIT 1;
    SELECT id INTO v_tipo_lab_ler FROM tipo_espacio_fisico WHERE codigo = 'LAB_ESTUDIOS_REGIONALES' LIMIT 1;
    SELECT id INTO v_tipo_lab_inchi FROM tipo_espacio_fisico WHERE codigo = 'LAB_INCHI' LIMIT 1;
    SELECT id INTO v_tipo_lab_meca FROM tipo_espacio_fisico WHERE codigo = 'LAB_MECATRONICA_REDES' LIMIT 1;
    SELECT id INTO v_tipo_lab_energias FROM tipo_espacio_fisico WHERE codigo = 'LAB_ENERGIAS_RENOVABLES' LIMIT 1;
    SELECT id INTO v_tipo_oficina FROM tipo_espacio_fisico WHERE codigo = 'OFICINA_DOCENTE' OR codigo = 'OFICINA_ADMIN' LIMIT 1;
    SELECT id INTO v_tipo_reuniones FROM tipo_espacio_fisico WHERE codigo = 'AMBITO_REUNION' OR codigo = 'AUDITORIO' LIMIT 1;
    SELECT id INTO v_tipo_estudio_tv FROM tipo_espacio_fisico WHERE codigo = 'ESTUDIO_RADIO_TV' LIMIT 1;
    SELECT id INTO v_tipo_sied FROM tipo_espacio_fisico WHERE codigo = 'SALA_VIDEOCONFERENCIA' LIMIT 1;
    SELECT id INTO v_tipo_mesa FROM tipo_espacio_fisico WHERE codigo = 'MESA_ENTRADAS' OR codigo = 'OFICINA_ADMIN' LIMIT 1;
    SELECT id INTO v_tipo_campo FROM tipo_espacio_fisico WHERE codigo = 'CAMPO_EXPERIMENTAL' LIMIT 1;
    SELECT id INTO v_tipo_galpon FROM tipo_espacio_fisico WHERE codigo = 'GALPON_CRIA' LIMIT 1;

    -- Limpiar espacios de inmuebles de demo previos
    DELETE FROM espacio_fisico WHERE inmueble_id IN (v_inm1_id, v_inm2_id, v_inm3_id);

    -- 1. Campus Universitario (Sede Los Sarmientos)
    INSERT INTO inmueble (
        id, institucion_id, numero_orden, nombre, domicilio, calle, numero,
        provincia_id, departamento_id, localidad_id, anio_construccion, anio_inicio_institucion,
        propiedad_institucion, condicion_tenencia, situacion, vincula_todas_carreras, es_maestro,
        superficie_cubierta_m2, superficie_total_m2, observaciones, verificado, estado_coneau, activo
    ) VALUES (
        v_inm1_id, v_inst_id, 1, 'Campus Universitario (Sede Los Sarmientos)', 'Ruta Los Peregrinos S/N, Los Sarmientos',
        'Ruta Los Peregrinos', 'S/N', v_prov_id, v_depto_id, v_loc_sarmientos_id, 2002, 2004,
        TRUE, 'PROPIO', 'Propiedad', TRUE, TRUE,
        1850.00, 3500.00, 'Validar con SGEF. Sede principal de campus universitario.', TRUE, 'CARGADO', TRUE
    ) ON CONFLICT (id) DO UPDATE SET
        numero_orden = EXCLUDED.numero_orden,
        nombre = EXCLUDED.nombre,
        calle = EXCLUDED.calle,
        numero = EXCLUDED.numero,
        anio_construccion = EXCLUDED.anio_construccion,
        anio_inicio_institucion = EXCLUDED.anio_inicio_institucion,
        situacion = EXCLUDED.situacion,
        vincula_todas_carreras = EXCLUDED.vincula_todas_carreras,
        es_maestro = EXCLUDED.es_maestro,
        estado_coneau = EXCLUDED.estado_coneau;

    -- Espacios del Inmueble 1 (Campus Los Sarmientos)
    INSERT INTO espacio_fisico (inmueble_id, tipo_espacio_id, denominacion, descripcion_infraestructura, cantidad, capacidad_personas_sentadas, capacidad_estudiantes, estado_coneau) VALUES
        (v_inm1_id, coalesce(v_tipo_auditorio, v_tipo_aula), 'Auditorio', 'Auditorio Jorge Omar Yoma', 1, 100, 100, 'CARGADO'),
        (v_inm1_id, v_tipo_aula, 'Aula (cap 20)', 'Aula (1-2-3-4-17-18)', 6, 20, 20, 'CARGADO'),
        (v_inm1_id, v_tipo_aula, 'Aula (cap 25)', 'Aula (5-11-12)', 3, 25, 25, 'CARGADO'),
        (v_inm1_id, v_tipo_aula, 'Aula (cap 35)', 'Aula (6-9-10-20-21)', 5, 35, 35, 'CARGADO'),
        (v_inm1_id, v_tipo_aula, 'Aula (cap 40)', 'Aula (7)', 1, 40, 40, 'CARGADO'),
        (v_inm1_id, v_tipo_aula, 'Aula (cap 50)', 'Aula (8)', 1, 50, 50, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_gabinete, v_tipo_aula), 'Gabinete de Enfermería - Aula', 'Gabinete de enfermeria', 1, 20, 20, 'CARGADO'),
        (v_inm1_id, v_tipo_aula, 'Aula (cap 41)', 'Aula (13-16)', 2, 41, 41, 'CARGADO'),
        (v_inm1_id, v_tipo_aula, 'Aula (cap 45)', 'Aula (14-15)', 2, 45, 45, 'CARGADO'),
        (v_inm1_id, v_tipo_aula, 'Aula (cap 60)', 'Aula (19-22) nuevas', 2, 60, 60, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_camara, v_tipo_aula), 'Sala de Audiencia y Cámara Gesell', 'Sala de Audiencia Camara gessel', 1, 20, 20, 'CARGADO'),
        (v_inm1_id, v_tipo_lab_inf, 'Laboratorio de Informatica (A-B)', 'Laboratorio de Informatica (A-B)', 2, 42, 42, 'CARGADO'),
        (v_inm1_id, v_tipo_lab_inf, 'Laboratorio de Informática C', 'Laboratorio de Informática C', 1, 12, 12, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_multiproposito, v_tipo_aula), 'Sala multipropósito', 'Sala multipropósito', 1, 160, 160, 'CARGADO'),
        (v_inm1_id, v_tipo_biblio, 'Biblioteca Central y Sala de Lectura', 'Biblioteca', 1, 40, 40, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_lab_quimica, v_tipo_aula), 'Laboratorio Química', 'Laborat. Quimica', 2, 15, 15, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_lab_fisica, v_tipo_aula), 'Laboratorio Física', 'Laborat. Fisica', 1, 15, 15, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_lab_micro, v_tipo_aula), 'Laboratorio Microscopía', 'Laborat. Microscopia', 1, 20, 20, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_lab_medios, v_tipo_aula), 'Laboratorio de Medios', 'Lab. de Comunicaciones', 1, 15, 15, 'CARGADO'),
        (v_inm1_id, v_tipo_lab_inf, 'Laboratorio de Programación y BD', 'Lab de Programación y BD', 1, 21, 21, 'CARGADO'),
        (v_inm1_id, v_tipo_lab_inf, 'Laboratorio Sist Operativos y Soft Libre', 'Lab SO y Software Libre', 1, 10, 10, 'CARGADO'),
        (v_inm1_id, v_tipo_lab_inf, 'Laboratorio PROGRESAR', 'Lab. PROGRESAR', 1, 15, 15, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_lab_lac, v_tipo_aula), 'Laboratorios LAC', 'Lab de Alta Complejidad', 7, 4, 4, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_lab_covid, v_tipo_aula), 'Laboratorio COVID', 'Lab de COVID', 1, 6, 6, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_lab_ler, v_tipo_aula), 'Laboratorios LER', 'Lab de Estudios Regionales', 9, 2, 2, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_lab_inchi, v_tipo_aula), 'Laboratorios INCHI', 'Lab. INCHI', 3, 2, 2, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_lab_meca, v_tipo_lab_inf), 'Laboratorio de Mecatrónica y Redes', 'Lab. de Mecatrónica y Redes (oficinas de sistemas PB)', 1, 15, 15, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_lab_energias, v_tipo_aula), 'Laboratorio de Energías Renovables', 'Lab. de Energías Renovables', 1, 10, 10, 'CARGADO'),
        (v_inm1_id, v_tipo_oficina, 'Oficina (cap 4)', 'Oficina de becas, oficina de investigadores PA, oficina de sistemas, oficina de medios PA', 4, 4, 4, 'CARGADO'),
        (v_inm1_id, v_tipo_oficina, 'Oficina (cap 1)', 'Oficina privada LAC, 5 oficinas de invest PA, oficina de académica', 7, 1, 1, 'CARGADO'),
        (v_inm1_id, v_tipo_oficina, 'Oficina (cap 3)', 'Oficina LAC, 7 oficinas institutos de investigacion, 5 oficinas de investigadores, despacho de alumnos', 14, 3, 3, 'CARGADO'),
        (v_inm1_id, v_tipo_oficina, 'Oficina (cap 2)', '2 Oficina Lab COVID, 2 oficinas INCHI, oficina de investigadores PB, 5 oficinas de dptos y secretarias, 2 oficinas de institutos de inv, 2 oficinas de docentes ivestigadores, 9 oficinas de escuelas y carreras, 3 oficinas academica y titulos, oficina de sistemas, 4 oficinas lab medios PB, 7 oficinas lab medios PA', 38, 2, 2, 'CARGADO'),
        (v_inm1_id, v_tipo_oficina, 'Oficina (cap 5)', 'Sala de RAC y oficina sistemas PB', 1, 5, 5, 'CARGADO'),
        (v_inm1_id, v_tipo_oficina, 'Oficina (cap 6)', 'Oficina académica Bedellía, oficina lab medios PB', 2, 6, 6, 'CARGADO'),
        (v_inm1_id, v_tipo_oficina, 'Oficina (cap 8)', '5 oficinas de sistemas PB, 4 oficinas de investigadores PA', 9, 8, 8, 'CARGADO'),
        (v_inm1_id, v_tipo_oficina, 'Oficina (cap 9)', 'Oficina Lab medios PA', 1, 9, 9, 'CARGADO'),
        (v_inm1_id, v_tipo_oficina, 'Oficina (cap 12)', '2 oficinas de investigadores PA', 2, 12, 12, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_reuniones, v_tipo_auditorio), 'Ámbito de reuniones', 'Oficinas institutos de investigadores sala de reunion', 1, 10, 10, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_estudio_tv, v_tipo_aula), 'Estudio de Radio y Técnicos', 'Lab medios PB', 1, 6, 6, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_estudio_tv, v_tipo_aula), 'Estudio Secundario', 'Lab medios PB', 1, 2, 2, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_estudio_tv, v_tipo_aula), 'Estudio de TV', 'Lab medios PB', 1, 20, 20, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_sied, v_tipo_aula), 'Sala de Videoconferencias SIED', 'Lab medios PB', 1, 15, 15, 'CARGADO'),
        (v_inm1_id, coalesce(v_tipo_mesa, v_tipo_oficina), 'Mesa de entradas', 'Lab medios PB e ingreso', 2, 2, 2, 'CARGADO');

    -- 2. Edificio Centro (Sede Centro)
    INSERT INTO inmueble (
        id, institucion_id, numero_orden, nombre, domicilio, calle, numero,
        provincia_id, departamento_id, localidad_id, anio_construccion, anio_inicio_institucion,
        propiedad_institucion, condicion_tenencia, situacion, vincula_todas_carreras, es_maestro,
        superficie_cubierta_m2, superficie_total_m2, observaciones, verificado, estado_coneau, activo
    ) VALUES (
        v_inm2_id, v_inst_id, 2, 'Edificio Centro (Sede Centro)', '9 de Julio 22, Chilecito',
        '9 de Julio', '22', v_prov_id, v_depto_id, v_loc_chilecito_id, 1930, 2004,
        TRUE, 'PROPIO', 'Propiedad', TRUE, TRUE,
        650.00, 800.00, 'Sede Centro histórica de la universidad.', TRUE, 'CARGADO', TRUE
    ) ON CONFLICT (id) DO UPDATE SET
        numero_orden = EXCLUDED.numero_orden,
        nombre = EXCLUDED.nombre,
        calle = EXCLUDED.calle,
        numero = EXCLUDED.numero,
        anio_construccion = EXCLUDED.anio_construccion,
        anio_inicio_institucion = EXCLUDED.anio_inicio_institucion,
        situacion = EXCLUDED.situacion,
        vincula_todas_carreras = EXCLUDED.vincula_todas_carreras,
        es_maestro = EXCLUDED.es_maestro,
        estado_coneau = EXCLUDED.estado_coneau;

    -- 3. Depósito Calle Provincial Nº 15
    INSERT INTO inmueble (
        id, institucion_id, numero_orden, nombre, domicilio, calle, numero,
        provincia_id, departamento_id, localidad_id, anio_construccion, anio_inicio_institucion,
        propiedad_institucion, condicion_tenencia, situacion, vincula_todas_carreras, es_maestro,
        superficie_cubierta_m2, superficie_total_m2, observaciones, verificado, estado_coneau, activo
    ) VALUES (
        v_inm3_id, v_inst_id, 3, 'Depósito Calle Provincial Nº 15', 'Ruta Provincial Nº 15 S/N, Chilecito',
        'Ruta Provincial Nº 15', 'S/N', v_prov_id, v_depto_id, v_loc_chilecito_id, 2005, NULL,
        FALSE, 'ALQUILER', 'Alquiler', FALSE, TRUE,
        280.00, 400.00, 'Depósito para logística de la institución.', TRUE, 'CARGADO', TRUE
    ) ON CONFLICT (id) DO UPDATE SET
        numero_orden = EXCLUDED.numero_orden,
        nombre = EXCLUDED.nombre,
        calle = EXCLUDED.calle,
        numero = EXCLUDED.numero,
        anio_construccion = EXCLUDED.anio_construccion,
        situacion = EXCLUDED.situacion,
        es_maestro = EXCLUDED.es_maestro,
        estado_coneau = EXCLUDED.estado_coneau;

    -- 4. Depósito Chilecito Centro
    INSERT INTO inmueble (
        id, institucion_id, numero_orden, nombre, domicilio, calle, numero,
        provincia_id, departamento_id, localidad_id, anio_construccion, anio_inicio_institucion,
        propiedad_institucion, condicion_tenencia, situacion, vincula_todas_carreras, es_maestro,
        superficie_cubierta_m2, superficie_total_m2, observaciones, verificado, estado_coneau, activo
    ) VALUES (
        v_inm4_id, v_inst_id, 4, 'Depósito Chilecito Centro', 'La Plata 268, Chilecito',
        'La Plata', '268', v_prov_id, v_depto_id, v_loc_chilecito_id, 1980, NULL,
        FALSE, 'ALQUILER', 'Alquiler', FALSE, TRUE,
        180.00, 220.00, 'Depósito en el casco céntrico.', TRUE, 'CARGADO', TRUE
    ) ON CONFLICT (id) DO UPDATE SET
        numero_orden = EXCLUDED.numero_orden,
        nombre = EXCLUDED.nombre,
        calle = EXCLUDED.calle,
        numero = EXCLUDED.numero,
        anio_construccion = EXCLUDED.anio_construccion,
        situacion = EXCLUDED.situacion,
        es_maestro = EXCLUDED.es_maestro,
        estado_coneau = EXCLUDED.estado_coneau;

    -- 5. Oficinas UNdeC (Sede Julián Amatte)
    INSERT INTO inmueble (
        id, institucion_id, numero_orden, nombre, domicilio, calle, numero,
        provincia_id, departamento_id, localidad_id, anio_construccion, anio_inicio_institucion,
        propiedad_institucion, condicion_tenencia, situacion, vincula_todas_carreras, es_maestro,
        superficie_cubierta_m2, superficie_total_m2, observaciones, verificado, estado_coneau, activo
    ) VALUES (
        v_inm5_id, v_inst_id, 5, 'Oficinas UNdeC (Sede Julián Amatte)', 'Julian Amatte 36, Chilecito',
        'Julian Amatte', '36', v_prov_id, v_depto_id, v_loc_chilecito_id, NULL, 2017,
        TRUE, 'PROPIO', 'Propiedad', FALSE, TRUE,
        220.00, 300.00, 'Oficinas y sala de reuniones institucionales.', TRUE, 'CARGADO', TRUE
    ) ON CONFLICT (id) DO UPDATE SET
        numero_orden = EXCLUDED.numero_orden,
        nombre = EXCLUDED.nombre,
        calle = EXCLUDED.calle,
        numero = EXCLUDED.numero,
        anio_inicio_institucion = EXCLUDED.anio_inicio_institucion,
        situacion = EXCLUDED.situacion,
        es_maestro = EXCLUDED.es_maestro,
        estado_coneau = EXCLUDED.estado_coneau;

    DELETE FROM espacio_fisico WHERE inmueble_id = v_inm5_id;
    INSERT INTO espacio_fisico (inmueble_id, tipo_espacio_id, denominacion, descripcion_infraestructura, cantidad, capacidad_personas_sentadas, capacidad_estudiantes, estado_coneau) VALUES
        (v_inm5_id, coalesce(v_tipo_reuniones, v_tipo_auditorio), 'Ámbito de reunión', 'Ámbito de reunión', 1, 10, 10, 'CARGADO');

    -- 6. Campo Experimental Malligasta
    INSERT INTO inmueble (
        id, institucion_id, numero_orden, nombre, domicilio, calle, numero,
        provincia_id, departamento_id, localidad_id, anio_construccion, anio_inicio_institucion,
        propiedad_institucion, condicion_tenencia, situacion, vincula_todas_carreras, es_maestro,
        superficie_cubierta_m2, superficie_total_m2, observaciones, verificado, estado_coneau, activo
    ) VALUES (
        v_inm6_id, v_inst_id, 6, 'Campo Experimental Malligasta', 'Calle Pública S/N, Malligasta',
        'Calle Pública', 'S/N', v_prov_id, v_depto_id, v_loc_malligasta_id, NULL, 2008,
        TRUE, 'PROPIO', 'Propiedad', FALSE, TRUE,
        350.00, 50000.00, 'Campo para prácticas agronómicas y biológicas.', TRUE, 'CARGADO', TRUE
    ) ON CONFLICT (id) DO UPDATE SET
        numero_orden = EXCLUDED.numero_orden,
        nombre = EXCLUDED.nombre,
        calle = EXCLUDED.calle,
        numero = EXCLUDED.numero,
        anio_inicio_institucion = EXCLUDED.anio_inicio_institucion,
        situacion = EXCLUDED.situacion,
        es_maestro = EXCLUDED.es_maestro,
        estado_coneau = EXCLUDED.estado_coneau;

    DELETE FROM espacio_fisico WHERE inmueble_id = v_inm6_id;
    INSERT INTO espacio_fisico (inmueble_id, tipo_espacio_id, denominacion, descripcion_infraestructura, cantidad, capacidad_personas_sentadas, capacidad_estudiantes, estado_coneau) VALUES
        (v_inm6_id, coalesce(v_tipo_campo, v_tipo_aula), 'Campo', 'Campo', 1, 50, 50, 'CARGADO');

    -- Vincular con carrera de Agronomía si existe
    IF v_carrera_agro_id IS NOT NULL THEN
        INSERT INTO inmueble_carreras (inmueble_id, carrera_id)
        VALUES (v_inm6_id, v_carrera_agro_id) ON CONFLICT DO NOTHING;
    END IF;

    -- 7. Campo Experimental Colonias Agrícolas de Tilimuqui
    INSERT INTO inmueble (
        id, institucion_id, numero_orden, nombre, domicilio, calle, numero,
        provincia_id, departamento_id, localidad_id, anio_construccion, anio_inicio_institucion,
        propiedad_institucion, condicion_tenencia, situacion, vincula_todas_carreras, es_maestro,
        superficie_cubierta_m2, superficie_total_m2, observaciones, verificado, estado_coneau, activo
    ) VALUES (
        v_inm7_id, v_inst_id, 7, 'Campo Experimental Colonias Agrícolas de Tilimuqui', 'Parcela Numero 9 S/N, Tilimuqui',
        'Parcela Numero 9', 'S/N', v_prov_id, v_depto_id, v_loc_tilimuqui_id, NULL, NULL,
        TRUE, 'PROPIO', 'Propiedad', FALSE, TRUE,
        150.00, 30000.00, 'Parcela productiva vinculada a Ing. Agronómica.', TRUE, 'CARGADO', TRUE
    ) ON CONFLICT (id) DO UPDATE SET
        numero_orden = EXCLUDED.numero_orden,
        nombre = EXCLUDED.nombre,
        calle = EXCLUDED.calle,
        numero = EXCLUDED.numero,
        situacion = EXCLUDED.situacion,
        es_maestro = EXCLUDED.es_maestro,
        estado_coneau = EXCLUDED.estado_coneau;

    DELETE FROM espacio_fisico WHERE inmueble_id = v_inm7_id;
    INSERT INTO espacio_fisico (inmueble_id, tipo_espacio_id, denominacion, descripcion_infraestructura, cantidad, capacidad_personas_sentadas, capacidad_estudiantes, estado_coneau) VALUES
        (v_inm7_id, coalesce(v_tipo_campo, v_tipo_aula), 'Campo', 'Campo', 1, 50, 50, 'CARGADO');

    IF v_carrera_agro_id IS NOT NULL THEN
        INSERT INTO inmueble_carreras (inmueble_id, carrera_id)
        VALUES (v_inm7_id, v_carrera_agro_id) ON CONFLICT DO NOTHING;
    END IF;

    -- 8. Colegio Nacional Agrotécnico "Ing. Julio C. Martínez"
    INSERT INTO inmueble (
        id, institucion_id, numero_orden, nombre, domicilio, calle, numero,
        provincia_id, departamento_id, localidad_id, anio_construccion, anio_inicio_institucion,
        propiedad_institucion, condicion_tenencia, situacion, vincula_todas_carreras, es_maestro,
        superficie_cubierta_m2, superficie_total_m2, observaciones, verificado, estado_coneau, activo
    ) VALUES (
        v_inm8_id, v_inst_id, 8, 'Colegio Nacional Agrotécnico "Ing. Julio C. Martínez"', 'Ramirez de Velasco S/N, Tilimuqui',
        'Ramirez de Velasco', 'S/N', v_prov_id, v_depto_id, v_loc_tilimuqui_id, 2010, 2010,
        FALSE, 'COMODATO', 'Usufructo', FALSE, TRUE,
        1200.00, 15000.00, 'Colegio agrotécnico dependiente de la universidad.', TRUE, 'CARGADO', TRUE
    ) ON CONFLICT (id) DO UPDATE SET
        numero_orden = EXCLUDED.numero_orden,
        nombre = EXCLUDED.nombre,
        calle = EXCLUDED.calle,
        numero = EXCLUDED.numero,
        anio_construccion = EXCLUDED.anio_construccion,
        anio_inicio_institucion = EXCLUDED.anio_inicio_institucion,
        situacion = EXCLUDED.situacion,
        es_maestro = EXCLUDED.es_maestro,
        estado_coneau = EXCLUDED.estado_coneau;

    -- 9. Residencia de docentes La Puntilla
    INSERT INTO inmueble (
        id, institucion_id, numero_orden, nombre, domicilio, calle, numero,
        provincia_id, departamento_id, localidad_id, anio_construccion, anio_inicio_institucion,
        propiedad_institucion, condicion_tenencia, situacion, vincula_todas_carreras, es_maestro,
        superficie_cubierta_m2, superficie_total_m2, observaciones, verificado, estado_coneau, activo
    ) VALUES (
        v_inm9_id, v_inst_id, 9, 'Residencia de docentes La Puntilla', '24 de Setiembre 294, La Puntilla',
        '24 de Setiembre', '294', v_prov_id, v_depto_id, v_loc_puntilla_id, NULL, 2008,
        TRUE, 'PROPIO', 'Propiedad', FALSE, TRUE,
        280.00, 500.00, 'Residencia para docentes visitantes y profesores de carreras de grado.', TRUE, 'CARGADO', TRUE
    ) ON CONFLICT (id) DO UPDATE SET
        numero_orden = EXCLUDED.numero_orden,
        nombre = EXCLUDED.nombre,
        calle = EXCLUDED.calle,
        numero = EXCLUDED.numero,
        anio_inicio_institucion = EXCLUDED.anio_inicio_institucion,
        situacion = EXCLUDED.situacion,
        es_maestro = EXCLUDED.es_maestro,
        estado_coneau = EXCLUDED.estado_coneau;

    -- 10. Campo Experimental Tilimuqui
    INSERT INTO inmueble (
        id, institucion_id, numero_orden, nombre, domicilio, calle, numero,
        provincia_id, departamento_id, localidad_id, anio_construccion, anio_inicio_institucion,
        propiedad_institucion, condicion_tenencia, situacion, vincula_todas_carreras, es_maestro,
        superficie_cubierta_m2, superficie_total_m2, observaciones, verificado, estado_coneau, activo
    ) VALUES (
        v_inm10_id, v_inst_id, 10, 'Campo Experimental Tilimuqui', 'Calle Pública S/N, Tilimuqui',
        'Calle Pública', 'S/N', v_prov_id, v_depto_id, v_loc_tilimuqui_id, 2013, 2016,
        TRUE, 'PROPIO', 'Propiedad', FALSE, TRUE,
        400.00, 45000.00, 'Campo experimental y productivo con galpones.', TRUE, 'CARGADO', TRUE
    ) ON CONFLICT (id) DO UPDATE SET
        numero_orden = EXCLUDED.numero_orden,
        nombre = EXCLUDED.nombre,
        calle = EXCLUDED.calle,
        numero = EXCLUDED.numero,
        anio_construccion = EXCLUDED.anio_construccion,
        anio_inicio_institucion = EXCLUDED.anio_inicio_institucion,
        situacion = EXCLUDED.situacion,
        es_maestro = EXCLUDED.es_maestro,
        estado_coneau = EXCLUDED.estado_coneau;

    DELETE FROM espacio_fisico WHERE inmueble_id = v_inm10_id;
    INSERT INTO espacio_fisico (inmueble_id, tipo_espacio_id, denominacion, descripcion_infraestructura, cantidad, capacidad_personas_sentadas, capacidad_estudiantes, estado_coneau) VALUES
        (v_inm10_id, coalesce(v_tipo_campo, v_tipo_aula), 'Campo experimental y productivo', 'Campo experimental y productivo', 1, 30, 30, 'CARGADO'),
        (v_inm10_id, coalesce(v_tipo_galpon, v_tipo_aula), 'Galpón para la cría de pollos', 'Galpón para la cría de pollos', 1, 0, 0, 'CARGADO');

    -- 11. Campus Deportivo San Miguel
    INSERT INTO inmueble (
        id, institucion_id, numero_orden, nombre, domicilio, calle, numero,
        provincia_id, departamento_id, localidad_id, anio_construccion, anio_inicio_institucion,
        propiedad_institucion, condicion_tenencia, situacion, vincula_todas_carreras, es_maestro,
        superficie_cubierta_m2, superficie_total_m2, observaciones, verificado, estado_coneau, activo
    ) VALUES (
        v_inm11_id, v_inst_id, 11, 'Campus Deportivo San Miguel', 'Ruta Provincial Nº 12 S/N, San Miguel',
        'Ruta Provincial Nº 12', 'S/N', v_prov_id, v_depto_id, v_loc_sanmiguel_id, 2018, NULL,
        TRUE, 'PROPIO', 'Propiedad', FALSE, TRUE,
        600.00, 25000.00, 'Predio deportivo y recreativo de la UNdeC.', TRUE, 'CARGADO', TRUE
    ) ON CONFLICT (id) DO UPDATE SET
        numero_orden = EXCLUDED.numero_orden,
        nombre = EXCLUDED.nombre,
        calle = EXCLUDED.calle,
        numero = EXCLUDED.numero,
        anio_construccion = EXCLUDED.anio_construccion,
        situacion = EXCLUDED.situacion,
        es_maestro = EXCLUDED.es_maestro,
        estado_coneau = EXCLUDED.estado_coneau;

    -- 12. Deposito Calle San Martin
    INSERT INTO inmueble (
        id, institucion_id, numero_orden, nombre, domicilio, calle, numero,
        provincia_id, departamento_id, localidad_id, anio_construccion, anio_inicio_institucion,
        propiedad_institucion, condicion_tenencia, situacion, vincula_todas_carreras, es_maestro,
        superficie_cubierta_m2, superficie_total_m2, observaciones, verificado, estado_coneau, activo
    ) VALUES (
        v_inm12_id, v_inst_id, 12, 'Deposito Calle San Martin', 'San Martin 41, Chilecito',
        'San Martin', '41', v_prov_id, v_depto_id, v_loc_chilecito_id, NULL, 2021,
        TRUE, 'PROPIO', 'Propiedad', FALSE, TRUE,
        150.00, 200.00, 'Depósito adicional sobre calle San Martín.', TRUE, 'CARGADO', TRUE
    ) ON CONFLICT (id) DO UPDATE SET
        numero_orden = EXCLUDED.numero_orden,
        nombre = EXCLUDED.nombre,
        calle = EXCLUDED.calle,
        numero = EXCLUDED.numero,
        anio_inicio_institucion = EXCLUDED.anio_inicio_institucion,
        situacion = EXCLUDED.situacion,
        es_maestro = EXCLUDED.es_maestro,
        estado_coneau = EXCLUDED.estado_coneau;

END $$;
