-- Paso intermediario obligatorio: Estándares por Carrera (Sección 2.2 Competencias/Ejes/Bloques y Sección 3 Carga Horaria)
-- + Designación unificada de Docente Responsable por Actividad Curricular

CREATE TABLE IF NOT EXISTS estandar (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    carrera_id UUID NOT NULL REFERENCES carrera(id) ON DELETE RESTRICT,
    nombre VARCHAR(180) NOT NULL,
    resolucion_ministerial VARCHAR(180),
    anio_vigencia INT,
    estado_vigencia VARCHAR(30) NOT NULL DEFAULT 'VIGENTE',
    tipo_seccion_22 VARCHAR(30) NOT NULL DEFAULT 'COMPETENCIAS',
    tipo_evaluacion_22 VARCHAR(30) NOT NULL DEFAULT 'MATRIZ_MULTIPLE',
    instrucciones_22 TEXT,
    observaciones TEXT,
    creado_por UUID REFERENCES usuario(id) ON DELETE SET NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_estandar_estado_vigencia
        CHECK (estado_vigencia IN ('VIGENTE', 'NO_VIGENTE')),
    CONSTRAINT chk_estandar_tipo_seccion_22
        CHECK (tipo_seccion_22 IN ('COMPETENCIAS', 'EJES', 'BLOQUES', 'NINGUNA')),
    CONSTRAINT chk_estandar_tipo_eval_22
        CHECK (tipo_evaluacion_22 IN ('ESCALA_UNICA', 'MATRIZ_MULTIPLE', 'NINGUNA'))
);

CREATE INDEX IF NOT EXISTS idx_estandar_carrera ON estandar(carrera_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_estandar_carrera_vigente
    ON estandar(carrera_id)
    WHERE estado_vigencia = 'VIGENTE';

CREATE TABLE IF NOT EXISTS estandar_escala_evaluacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    estandar_id UUID NOT NULL REFERENCES estandar(id) ON DELETE CASCADE,
    orden INT NOT NULL,
    codigo VARCHAR(60) NOT NULL,
    etiqueta VARCHAR(120) NOT NULL,
    descripcion_referencia TEXT,
    es_valor_por_defecto BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_estandar_escala_codigo UNIQUE (estandar_id, codigo)
);

CREATE INDEX IF NOT EXISTS idx_estandar_escala_estandar ON estandar_escala_evaluacion(estandar_id);

CREATE TABLE IF NOT EXISTS estandar_grupo_22 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    estandar_id UUID NOT NULL REFERENCES estandar(id) ON DELETE CASCADE,
    orden INT NOT NULL,
    codigo VARCHAR(60) NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    descripcion TEXT,
    habilitado BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_estandar_grupo_22_estandar ON estandar_grupo_22(estandar_id);

CREATE TABLE IF NOT EXISTS estandar_item_22 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grupo_id UUID NOT NULL REFERENCES estandar_grupo_22(id) ON DELETE CASCADE,
    orden INT NOT NULL,
    enunciado TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_estandar_item_22_grupo ON estandar_item_22(grupo_id);

CREATE TABLE IF NOT EXISTS estandar_area_carga_horaria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    estandar_id UUID NOT NULL REFERENCES estandar(id) ON DELETE CASCADE,
    orden INT NOT NULL,
    codigo VARCHAR(60) NOT NULL,
    nombre VARCHAR(180) NOT NULL,
    etiqueta_practica VARCHAR(220),
    habilitada BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_estandar_area_codigo UNIQUE (estandar_id, codigo)
);

CREATE INDEX IF NOT EXISTS idx_estandar_area_estandar ON estandar_area_carga_horaria(estandar_id);

CREATE TABLE IF NOT EXISTS actividad_curricular_evaluacion_22 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actividad_curricular_id UUID NOT NULL REFERENCES actividad_curricular_plan(id) ON DELETE CASCADE,
    estandar_item_id UUID NOT NULL REFERENCES estandar_item_22(id) ON DELETE CASCADE,
    escala_evaluacion_id UUID NOT NULL REFERENCES estandar_escala_evaluacion(id) ON DELETE CASCADE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_act_eval_22 UNIQUE (actividad_curricular_id, estandar_item_id, escala_evaluacion_id)
);

CREATE INDEX IF NOT EXISTS idx_act_eval_22_act ON actividad_curricular_evaluacion_22(actividad_curricular_id);

CREATE TABLE IF NOT EXISTS actividad_curricular_carga_horaria_area (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actividad_curricular_id UUID NOT NULL REFERENCES actividad_curricular_plan(id) ON DELETE CASCADE,
    area_carga_horaria_id UUID NOT NULL REFERENCES estandar_area_carga_horaria(id) ON DELETE CASCADE,
    horas_total_presencial INT NOT NULL DEFAULT 0,
    horas_total_distancia INT NOT NULL DEFAULT 0,
    horas_practica_presencial INT NOT NULL DEFAULT 0,
    horas_practica_distancia INT NOT NULL DEFAULT 0,

    CONSTRAINT uq_act_carga_area UNIQUE (actividad_curricular_id, area_carga_horaria_id)
);

CREATE INDEX IF NOT EXISTS idx_act_carga_area_act ON actividad_curricular_carga_horaria_area(actividad_curricular_id);

CREATE TABLE IF NOT EXISTS actividad_curricular_carga_horaria_resumen (
    actividad_curricular_id UUID PRIMARY KEY REFERENCES actividad_curricular_plan(id) ON DELETE CASCADE,
    ambitos_formacion_practica TEXT,
    horas_semanal_total_presencial INT NOT NULL DEFAULT 0,
    horas_semanal_total_distancia INT NOT NULL DEFAULT 0,
    horas_semanal_practica_presencial INT NOT NULL DEFAULT 0,
    horas_semanal_practica_distancia INT NOT NULL DEFAULT 0,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE plan_estudio_ft001
    ADD COLUMN IF NOT EXISTS estandar_id UUID REFERENCES estandar(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_plan_estudio_estandar ON plan_estudio_ft001(estandar_id);

ALTER TABLE actividad_curricular_plan
    ADD COLUMN IF NOT EXISTS docente_responsable_id UUID REFERENCES usuario(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS docente_responsable_nombre VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_actividad_docente_responsable ON actividad_curricular_plan(docente_responsable_id);

UPDATE actividad_curricular_plan acp
SET docente_responsable_id = paa.docente_designado_id,
    docente_responsable_nombre = paa.docente_designado_nombre
FROM programa_analitico_asignatura paa
WHERE paa.actividad_curricular_id = acp.id
  AND paa.docente_designado_id IS NOT NULL
  AND acp.docente_responsable_id IS NULL;

-- Backfill de estándares de prueba VIGENTE para carreras que ya poseen planes de estudio
DO $$
DECLARE
    rec RECORD;
    v_estandar_id UUID;
    v_grupo_id UUID;
    v_grupo_app_id UUID;
    v_grupo_prof_id UUID;
BEGIN
    FOR rec IN
        SELECT DISTINCT c.id, c.nombre
        FROM carrera c
        WHERE EXISTS (SELECT 1 FROM plan_estudio_ft001 p WHERE p.carrera_id = c.id)
    LOOP
        SELECT id INTO v_estandar_id
        FROM estandar
        WHERE carrera_id = rec.id AND estado_vigencia = 'VIGENTE'
        LIMIT 1;

        IF v_estandar_id IS NULL THEN
            v_estandar_id := gen_random_uuid();

            IF LOWER(rec.nombre) LIKE '%mecatr%' THEN
                INSERT INTO estandar (
                    id, carrera_id, nombre, resolucion_ministerial, anio_vigencia, estado_vigencia,
                    tipo_seccion_22, tipo_evaluacion_22, instrucciones_22, observaciones
                ) VALUES (
                    v_estandar_id,
                    rec.id,
                    'Estándar CONEAU — ' || rec.nombre,
                    'Res. Ministerial Inicial (Prueba)',
                    2026,
                    'VIGENTE',
                    'EJES',
                    'ESCALA_UNICA',
                    'Si la actividad curricular prepara al alumno para su adquisición como fija la Resolución Ministerial, seleccione el grado de profundidad en el tratamiento de los mismos. Si no prepara al alumno, deje el cuadro en "Ninguna".',
                    'Estándar de referencia inicial generado para planes existentes.'
                );

                INSERT INTO estandar_escala_evaluacion (estandar_id, orden, codigo, etiqueta, descripcion_referencia, es_valor_por_defecto) VALUES
                    (v_estandar_id, 1, 'NINGUNA', '(Ninguna)', 'La actividad curricular no aborda este eje.', TRUE),
                    (v_estandar_id, 2, 'BAJO', 'Bajo', 'Abordaje introductorio o superficial del eje.', FALSE),
                    (v_estandar_id, 3, 'MEDIO', 'Medio', 'Abordaje intermedio con aplicación práctica del eje.', FALSE),
                    (v_estandar_id, 4, 'ALTO', 'Alto', 'Abordaje profundo e integrador del eje.', FALSE);

                v_grupo_id := gen_random_uuid();
                INSERT INTO estandar_grupo_22 (id, estandar_id, orden, codigo, titulo, descripcion, habilitado)
                VALUES (v_grupo_id, v_estandar_id, 1, 'EJES_TRANSVERSALES', 'Ejes y enunciados multidimensionales y transversales', 'Ejes transversales definidos para la carrera.', TRUE);

                INSERT INTO estandar_item_22 (grupo_id, orden, enunciado) VALUES
                    (v_grupo_id, 1, 'Análisis de la funcionalidad y aplicabilidad de máquinas, equipos, dispositivos, instalaciones y sistemas.'),
                    (v_grupo_id, 2, 'Concepción, cálculo e implementación de soluciones tecnológicas integradas.'),
                    (v_grupo_id, 3, 'Dirección y control de los procesos de operación y mantenimiento.'),
                    (v_grupo_id, 4, 'Identificación, utilización y selección de las técnicas y herramientas disponibles.'),
                    (v_grupo_id, 5, 'Evaluación del funcionamiento y condiciones de uso de dispositivos o sistemas de acuerdo con especificaciones.'),
                    (v_grupo_id, 6, 'Proyecto, dirección, supervisión y control de lo referido a higiene y seguridad.'),
                    (v_grupo_id, 7, 'Identificación, formulación y resolución de problemas propios de la disciplina.');
            ELSE
                INSERT INTO estandar (
                    id, carrera_id, nombre, resolucion_ministerial, anio_vigencia, estado_vigencia,
                    tipo_seccion_22, tipo_evaluacion_22, instrucciones_22, observaciones
                ) VALUES (
                    v_estandar_id,
                    rec.id,
                    'Estándar CONEAU — ' || rec.nombre,
                    'Res. Ministerial Inicial (Prueba)',
                    2026,
                    'VIGENTE',
                    'COMPETENCIAS',
                    'MATRIZ_MULTIPLE',
                    'Seleccione para cada contenido o habilidad los niveles alcanzados por el estudiante (Aprende, Observa, Resuelve, Ejecuta).',
                    'Estándar de referencia inicial generado para planes existentes.'
                );

                INSERT INTO estandar_escala_evaluacion (estandar_id, orden, codigo, etiqueta, descripcion_referencia, es_valor_por_defecto) VALUES
                    (v_estandar_id, 1, 'APRENDE', 'Aprende', 'El alumno aprende los fundamentos teóricos relacionados con el contenido/habilidad', FALSE),
                    (v_estandar_id, 2, 'OBSERVA', 'Observa', 'El alumno observa actividades demostrativas relacionadas con el contenido/habilidad', FALSE),
                    (v_estandar_id, 3, 'RESUELVE', 'Resuelve', 'El alumno resuelve problemas teórico-prácticos relacionados con el contenido/habilidad', FALSE),
                    (v_estandar_id, 4, 'EJECUTA', 'Ejecuta', 'El alumno ejecuta, al menos una vez, las acciones relacionadas con el contenido/habilidad', FALSE);

                v_grupo_id := gen_random_uuid();
                INSERT INTO estandar_grupo_22 (id, estandar_id, orden, codigo, titulo, descripcion, habilitado)
                VALUES (v_grupo_id, v_estandar_id, 1, 'FORMACION_BASICA', 'Formación Básica', 'Contenidos y habilidades de formación básica.', TRUE);

                INSERT INTO estandar_item_22 (grupo_id, orden, enunciado) VALUES
                    (v_grupo_id, 1, 'Lógica matemática, álgebra, cálculo diferencial e integral y geometría analítica.'),
                    (v_grupo_id, 2, 'Fundamentos de física general y aplicada.'),
                    (v_grupo_id, 3, 'Estadística descriptiva, probabilidad, inferencia estadística y diseño experimental.'),
                    (v_grupo_id, 4, 'Fundamentos de química general, inorgánica y orgánica aplicada.');

                v_grupo_app_id := gen_random_uuid();
                INSERT INTO estandar_grupo_22 (id, estandar_id, orden, codigo, titulo, descripcion, habilitado)
                VALUES (v_grupo_app_id, v_estandar_id, 2, 'FORMACION_APLICADA', 'Formación Aplicada', 'Contenidos y habilidades de formación aplicada.', TRUE);

                INSERT INTO estandar_item_22 (grupo_id, orden, enunciado) VALUES
                    (v_grupo_app_id, 1, 'Análisis de sistemas, modelado y metodologías experimentales de la disciplina.'),
                    (v_grupo_app_id, 2, 'Manejo de instrumental, tecnologías y herramientas específicas del área.'),
                    (v_grupo_app_id, 3, 'Desarrollo sustentable, legislación, economía y gestión de proyectos.');

                v_grupo_prof_id := gen_random_uuid();
                INSERT INTO estandar_grupo_22 (id, estandar_id, orden, codigo, titulo, descripcion, habilitado)
                VALUES (v_grupo_prof_id, v_estandar_id, 3, 'FORMACION_PROFESIONAL', 'Formación Profesional', 'Contenidos y habilidades de desempeño profesional.', TRUE);

                INSERT INTO estandar_item_22 (grupo_id, orden, enunciado) VALUES
                    (v_grupo_prof_id, 1, 'Gestión integral y resolución autónoma de problemas profesionales.'),
                    (v_grupo_prof_id, 2, 'Planificación, evaluación y dirección técnica en contextos reales.');
            END IF;

            INSERT INTO estandar_area_carga_horaria (estandar_id, orden, codigo, nombre, etiqueta_practica, habilitada) VALUES
                (v_estandar_id, 1, 'FORMACION_BASICA', 'Formación Básica', 'Formación Básica', TRUE),
                (v_estandar_id, 2, 'FORMACION_APLICADA', 'Formación Aplicada', 'Formación Aplicada', TRUE),
                (v_estandar_id, 3, 'FORMACION_PROFESIONAL', 'Formación Profesional', 'Formación Profesional', TRUE),
                (v_estandar_id, 4, 'FORMACION_COMPLEMENTARIA', 'Formación Complementaria', 'Carga horaria asignada a formación complementaria', TRUE);
        END IF;

        UPDATE plan_estudio_ft001
        SET estandar_id = v_estandar_id
        WHERE carrera_id = rec.id
          AND estandar_id IS NULL;
    END LOOP;
END $$;
