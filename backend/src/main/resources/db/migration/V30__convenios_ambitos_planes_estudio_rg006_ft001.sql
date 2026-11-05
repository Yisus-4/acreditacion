-- =============================================================================
-- V30__convenios_ambitos_planes_estudio_rg006_ft001.sql
-- Maestro de Convenios Institucionales (RG-007)
-- Maestro de Ámbitos de Formación Práctica (RG-006)
-- Planes de Estudio (FT-001) y Malla Curricular con Correlatividades
-- Programas Analíticos de Asignaturas (Actividad 8.4)
-- =============================================================================

-- 1. MAESTRO DE CONVENIOS INSTITUCIONALES (RG-007)
CREATE TABLE IF NOT EXISTS convenio_institucional (
    id UUID PRIMARY KEY,
    expediente VARCHAR(100),
    resolucion_rectoral VARCHAR(150),
    contraparte VARCHAR(255) NOT NULL,
    fecha_celebracion DATE,
    duracion VARCHAR(150),
    fecha_cese DATE,
    objeto TEXT,
    vigente BOOLEAN NOT NULL DEFAULT TRUE,
    estado_vigencia VARCHAR(50) DEFAULT 'VIGENTE',
    estado_ejecucion VARCHAR(100),
    financiamiento VARCHAR(150),
    responsable_undec VARCHAR(150),
    objetivos_generales TEXT,
    objetivos_especificos TEXT,
    resultados_esperados TEXT,
    carreras_beneficiadas TEXT,
    tipo_convenio VARCHAR(100),
    archivo_id UUID,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_convenio_contraparte ON convenio_institucional(contraparte);
CREATE INDEX IF NOT EXISTS idx_convenio_vigente ON convenio_institucional(vigente);

-- 2. MAESTRO DE ÁMBITOS DE FORMACIÓN PRÁCTICA (RG-006)
-- Categorías: INTERNO, EXTERNO, UNIDAD_DEMOSTRATIVA, CAM
CREATE TABLE IF NOT EXISTS ambito_practica (
    id UUID PRIMARY KEY,
    numero_orden INTEGER,
    denominacion VARCHAR(255) NOT NULL,
    tipo_categoria VARCHAR(50) NOT NULL,
    tipo_ambito VARCHAR(100),
    tipo_establecimiento VARCHAR(100),
    inmueble_id UUID REFERENCES inmueble(id) ON DELETE SET NULL,
    inmueble_nombre_referencia VARCHAR(255),
    convenio_id UUID REFERENCES convenio_institucional(id) ON DELETE SET NULL,
    descripcion TEXT,
    anio_construccion INTEGER,
    superficie_m2 NUMERIC(12,2),
    capacidad_alumnos INTEGER,
    disponibilidad_horaria VARCHAR(100),
    -- Campos específicos CAM
    direccion VARCHAR(255),
    localidad VARCHAR(100),
    provincia VARCHAR(100),
    departamento VARCHAR(100),
    codigo_postal VARCHAR(20),
    tipo_dependencia VARCHAR(100),
    sector VARCHAR(100),
    actividad_alumno_cam TEXT,
    -- Campos específicos Unidades Demostrativas
    especies_animales_cultivos TEXT,
    numero_animales INTEGER,
    actividades_demostrativas TEXT,
    carreras_que_lo_usan TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ambito_categoria ON ambito_practica(tipo_categoria);
CREATE INDEX IF NOT EXISTS idx_ambito_inmueble ON ambito_practica(inmueble_id);
CREATE INDEX IF NOT EXISTS idx_ambito_convenio ON ambito_practica(convenio_id);

-- 3. SELECCIÓN DE ÁMBITOS POR CARRERA EN AUTOEVALUACIÓN (Snapshot / Vínculo carrera)
CREATE TABLE IF NOT EXISTS autoevaluacion_carrera_ambito (
    id UUID PRIMARY KEY,
    proceso_id UUID NOT NULL REFERENCES proceso_acreditacion(id) ON DELETE CASCADE,
    ambito_id UUID NOT NULL REFERENCES ambito_practica(id) ON DELETE CASCADE,
    utilizado_por_carrera BOOLEAN NOT NULL DEFAULT TRUE,
    asignaturas_vinculadas TEXT,
    responsable_practica VARCHAR(150),
    horarios_uso_anio_anterior VARCHAR(100),
    tipo_practica_estudiantes TEXT,
    observaciones_carrera TEXT,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_proceso_ambito UNIQUE (proceso_id, ambito_id)
);

CREATE INDEX IF NOT EXISTS idx_autoeval_ambito_proc ON autoevaluacion_carrera_ambito(proceso_id);

-- 4. PLANES DE ESTUDIO (FT-001)
CREATE TABLE IF NOT EXISTS plan_estudio_ft001 (
    id UUID PRIMARY KEY,
    carrera_id UUID NOT NULL REFERENCES carrera(id) ON DELETE CASCADE,
    proceso_id UUID REFERENCES proceso_acreditacion(id) ON DELETE SET NULL,
    codigo_resolucion VARCHAR(150),
    anio_aprobacion INTEGER,
    anio_inicio INTEGER,
    duracion_anios INTEGER,
    orientaciones_json TEXT,
    fundamentacion_modificaciones TEXT,
    estructura_sintetica TEXT,
    horas_presenciales INTEGER,
    horas_no_presenciales INTEGER,
    horas_totales INTEGER,
    horas_optativas_minimas INTEGER,
    actividades_comunes_ciclo TEXT,
    titulo_intermedio_denominacion VARCHAR(200),
    titulo_intermedio_duracion VARCHAR(100),
    titulo_intermedio_actividades TEXT,
    instancia_final_graduacion TEXT,
    otra_informacion TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_plan_estudio_carrera ON plan_estudio_ft001(carrera_id);

-- 5. ACTIVIDADES CURRICULARES (ASIGNATURAS DE LA MALLA)
CREATE TABLE IF NOT EXISTS actividad_curricular_plan (
    id UUID PRIMARY KEY,
    plan_estudio_id UUID NOT NULL REFERENCES plan_estudio_ft001(id) ON DELETE CASCADE,
    numero_orden INTEGER NOT NULL,
    codigo VARCHAR(50),
    nombre VARCHAR(255) NOT NULL,
    grupo VARCHAR(100), -- 'C.B.I.', 'T.B.', 'T.A.', 'C.T.C.'
    subgrupo VARCHAR(100), -- '1º', '2º', '3º', '4º', '5º'
    horas_reloj INTEGER,
    horas_presenciales INTEGER,
    horas_no_presenciales INTEGER,
    correlativas TEXT,
    es_obligatoria BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_actividad_curricular_plan ON actividad_curricular_plan(plan_estudio_id);

-- 6. PROGRAMAS ANALÍTICOS DE ASIGNATURAS (ACTIVIDAD 8.4)
CREATE TABLE IF NOT EXISTS programa_analitico_asignatura (
    id UUID PRIMARY KEY,
    proceso_id UUID NOT NULL REFERENCES proceso_acreditacion(id) ON DELETE CASCADE,
    actividad_curricular_id UUID REFERENCES actividad_curricular_plan(id) ON DELETE SET NULL,
    nombre_asignatura VARCHAR(255) NOT NULL,
    anio_cursada VARCHAR(50),
    resolucion_aprobacion VARCHAR(150),
    archivo_id UUID,
    archivo_nombre VARCHAR(255),
    archivo_ruta VARCHAR(500),
    tamanio_bytes BIGINT,
    mime_type VARCHAR(100),
    estado VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE', -- 'CARGADO', 'PENDIENTE', 'APROBADO'
    observaciones TEXT,
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prog_analitico_proc ON programa_analitico_asignatura(proceso_id);
