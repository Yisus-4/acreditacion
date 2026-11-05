-- =============================================================================
-- V31__seed_convenios_ambitos_plan_agrimensura.sql
-- Datos iniciales reales UNdeC:
-- 1. Convenios Institucionales Oficiales (RG-007)
-- 2. Ámbitos de Práctica Oficiales (RG-006: Internos, Demostrativos y CAMs con FK)
-- 3. Plan de Estudios FT-001 de Ingeniería en Agrimensura (Ord. HCS 015/24)
-- 4. Las 46 Asignaturas Oficiales con Ciclos y Correlatividades
-- =============================================================================

DO $$
DECLARE
    -- IDs de Convenios
    c_clinica_san_nicolas_id UUID := '77700001-0000-0000-0000-000000000001';
    c_hospital_el_cruce_id   UUID := '77700001-0000-0000-0000-000000000002';
    c_hospital_madre_nino_id UUID := '77700001-0000-0000-0000-000000000003';
    c_hospital_carrillo_id   UUID := '77700001-0000-0000-0000-000000000004';
    c_hospital_onativia_id   UUID := '77700001-0000-0000-0000-000000000005';
    c_hospital_motta_id      UUID := '77700001-0000-0000-0000-000000000006';
    c_hospital_vera_barros_id UUID := '77700001-0000-0000-0000-000000000007';
    c_zona_sanitaria3_id     UUID := '77700001-0000-0000-0000-000000000008';
    c_munay_id               UUID := '77700001-0000-0000-0000-000000000009';
    c_pami_id                UUID := '77700001-0000-0000-0000-000000000010';
    c_iucbc_id               UUID := '77700001-0000-0000-0000-000000000011';
    c_col_agrimensores_id    UUID := '77700001-0000-0000-0000-000000000012';
    c_unca_tecnologia_id     UUID := '77700001-0000-0000-0000-000000000013';
    c_agrosuma_id            UUID := '77700001-0000-0000-0000-000000000014';
    c_finca_san_nicolas_id   UUID := '77700001-0000-0000-0000-000000000015';
    c_sec_tierras_id         UUID := '77700001-0000-0000-0000-000000000016';
    c_inta_id                UUID := '77700001-0000-0000-0000-000000000017';
    c_segemar_id             UUID := '77700001-0000-0000-0000-000000000018';

    -- Inmueble Los Sarmientos
    v_inmueble_sarmientos_id UUID;
    v_inmueble_tilimuqui_id  UUID;

    -- Carrera y Plan
    v_carrera_agrim_id UUID;
    v_plan_agrim_id UUID := '88800001-0000-0000-0000-000000000001';
BEGIN
    -- 1. SEED DE CONVENIOS INSTITUCIONALES (RG-007)
    INSERT INTO convenio_institucional (
        id, expediente, resolucion_rectoral, contraparte, fecha_celebracion, duracion,
        objeto, vigente, estado_vigencia, estado_ejecucion, financiamiento, responsable_undec,
        carreras_beneficiadas, tipo_convenio
    ) VALUES
    (c_col_agrimensores_id, '659/2025', '457/25 09/06/2025', 'Colegio de Agrimensores de La Rioja', '2025-05-30', '1 año con renovación automática',
     'Protocolo Específico de Prácticas PreProfesionales para alumnos de Ingeniería en Agrimensura y cooperación permanente.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Ab. César Alberto Salcedo', 'Ingeniería en Agrimensura', 'Prácticas y Pasantías'),
    
    (c_unca_tecnologia_id, '660/2025', '458/25 09/06/2025', 'Facultad de Tecnología y Cs. Aplicadas - UNCa', '2025-05-30', '3 años prorrogable automáticamente',
     'Cooperación académica, proyectos conjuntos de investigación, movilidad estudiantil y desarrollo curricular.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Ab. César Alberto Salcedo', 'Ingeniería en Agrimensura', 'Cooperación Académica'),

    (c_sec_tierras_id, '464/2022', '422/2022 27/07/2022', 'Secretaría de Tierras - Min. de Vivienda y Tierras de La Rioja', '2022-07-05', '3 años con renovación automática',
     'Realización de prácticas pre-profesionales de agrimensura, catastro territorial y relevamiento parcelario.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Ab. César Salcedo', 'Ingeniería en Agrimensura', 'Prácticas y Pasantías'),

    (c_segemar_id, '535/2017', '330/17 22/06/2017', 'Servicio Geológico Minero (SEGEMAR)', '2017-06-13', '3 años prorrogable automáticamente',
     'Pasantías y prácticas de campo en geodesia, cartografía, geomorfología y teledetección satelital.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Cr. Fortes', 'Ingeniería en Agrimensura', 'Prácticas y Pasantías'),

    (c_clinica_san_nicolas_id, 'MEX - S01 25/2017', '303/17 08/06/2017', 'Clínica San Nicolás S.R.L.', '2017-06-07', '3 años prorrogable automáticamente',
     'Convenio Marco de Cooperación y Prácticas Pre-Profesionales en Centro de Salud privado.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Lic. Mirtha Seco', 'Licenciatura en Enfermería', 'Prácticas Sanitarias'),

    (c_hospital_el_cruce_id, 'MEX-S01: 0000024/2017', '312/19 31/05/2019', 'Hospital de Alta Complejidad El Cruce en Red', '2019-05-27', '1 año renovable automáticamente',
     'Prácticas pre-profesionales de cuidados críticos, alta complejidad, cardiología, respiratorio y neonatología.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Lic. Mirtha Seco', 'Licenciatura en Enfermería', 'Prácticas Hospitalarias'),

    (c_hospital_madre_nino_id, 'EXP-S01: 0001279/2018', '073/19 20/02/2019', 'Hospital de la Madre y el Niño (La Rioja)', '2019-02-19', '3 años prorrogable automáticamente',
     'Prácticas en enfermería materno-infantil, cuidados pediátricos y terapia intensiva infantil.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Lic. Mirta Seco', 'Licenciatura en Enfermería', 'Prácticas Hospitalarias'),

    (c_hospital_carrillo_id, '811/2018', '492-18 16/08/18', 'Hospital Distrital Ramón Carrillo - Famatina', '2018-08-15', '3 años prorrogable automáticamente',
     'Prácticas de salud comunitaria, guardia y atención primaria de la salud.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Lic. Mirta Seco', 'Licenciatura en Enfermería', 'Prácticas Hospitalarias'),

    (c_hospital_onativia_id, '396/2018', '735/18 06/11/2018', 'Hospital Dr. Arturo Oñativia', '2018-10-18', '3 años prorrogable automáticamente',
     'Cuidados intensivos de adultos y emergencias médicas hospitalarias.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Lic. Mirta Seco', 'Licenciatura en Enfermería', 'Prácticas Hospitalarias'),

    (c_hospital_motta_id, '1213/2016', '638/16 14/11/2016', 'Hospital Eleazar Herrera Motta (Chilecito)', '2016-11-09', '3 años prorrogables automáticamente',
     'Rotación por clínica médica, quirúrgica, traumatología, obstetricia y UTI.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Rectorado', 'Licenciatura en Enfermería', 'Prácticas Hospitalarias'),

    (c_hospital_vera_barros_id, 'EXP-S01: 0001279/2018', '073/19 20/02/2019', 'Hospital Regional Dr. Enrique Vera Barros', '2019-02-19', '3 años prorrogable automáticamente',
     'Prácticas de adultos y ancianos en cuidados intermedios e intensivos y guardia central.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Lic. Mirta Seco', 'Licenciatura en Enfermería', 'Prácticas Hospitalarias'),

    (c_zona_sanitaria3_id, 'Mex-S01: 23/2017', '277/18 17/05/2018', 'Región Zona Sanitaria III', '2018-05-16', '3 años prorrogable automáticamente',
     'Promoción de la salud comunitaria y prevención de enfermedades en el Departamento Chilecito.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Lic. Mirta Seco', 'Licenciatura en Enfermería', 'Salud Comunitaria'),

    (c_munay_id, 'EXP-S01: 0000330/2019', '184/19 08/04/2019', 'Centro Terapéutico de Adicciones MUNAY', '2019-04-03', '3 años prorrogable automáticamente',
     'Prácticas en salud mental y abordaje de consumos problemáticos.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Lic. Mirta Seco', 'Licenciatura en Enfermería', 'Salud Mental'),

    (c_pami_id, '994/2022', '614/22 23/09/2022', 'PAMI - INSSJP', '2022-09-20', '2 años prorrogable automáticamente',
     'Contención y cuidados de adultos mayores, geriatría y promoción comunitaria.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Rectorado', 'Licenciatura en Enfermería', 'Gerontología'),

    (c_iucbc_id, '1460/2022', '872/22 29/11/2022', 'Instituto Univ. de Ciencias Biomédicas de Córdoba (IUCBC)', '2022-11-23', '5 años',
     'Colaboración científica, docencia compartida y perfeccionamiento en ciencias de la salud.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Rectorado', 'Licenciatura en Enfermería', 'Cooperación Académica'),

    (c_inta_id, '1030/2023', '395/24 07/06/2024', 'Instituto Nacional de Tecnología Agropecuaria (INTA)', '2024-06-04', '5 años',
     'Cooperación científica, técnica y académica en unidades demostrativas y campos experimentales.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Lic. Lorena Martinez', 'Ingeniería Agronómica / Cs. Naturales', 'Cooperación Técnica'),

    (c_agrosuma_id, '355/2024', '714/24 30/09/2024', 'Agrosuma S.R.L.', '2024-09-18', '1 año con renovación automática',
     'Prácticas profesionales supervisadas y pasantías en parcelas y explotaciones agrícolas.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Dra. Mariana Varas', 'Ingeniería Agronómica', 'Prácticas y Pasantías'),

    (c_finca_san_nicolas_id, '625/24', '916/24 09/12/2024', 'Finca San Nicolás S.A.', '2024-11-15', '1 año con renovación automática',
     'Prácticas pre-profesionales y de campo vinculadas a producción vegetal y enología.', TRUE, 'VIGENTE', 'en ejecución', 'no requiere fondos', 'Dra. Mariana Varas', 'Ingeniería Agronómica', 'Prácticas y Pasantías')
    ON CONFLICT (id) DO NOTHING;

    -- Localizar inmuebles de referencia
    SELECT id INTO v_inmueble_sarmientos_id FROM inmueble WHERE nombre ILIKE '%sarmiento%' LIMIT 1;
    SELECT id INTO v_inmueble_tilimuqui_id FROM inmueble WHERE nombre ILIKE '%tilimuqui%' LIMIT 1;

    -- 2. SEED DE ÁMBITOS DE PRÁCTICA (RG-006)
    -- Ámbitos Internos (1 a 19)
    INSERT INTO ambito_practica (
        id, numero_orden, denominacion, tipo_categoria, tipo_ambito, tipo_establecimiento,
        inmueble_id, inmueble_nombre_referencia, descripcion, anio_construccion, superficie_m2,
        capacidad_alumnos, disponibilidad_horaria, carreras_que_lo_usan
    ) VALUES
    ('66600001-0000-0000-0000-000000000001', 1, 'Gabinete de Enfermería', 'INTERNO', 'Gabinete de simulación', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Espacio físico compuesto por mobiliario para práctica simulada de técnicas de enfermería.', 2011, 90.0, 20, '08 a 22 hs', 'Licenciatura en Enfermería'),

    ('66600001-0000-0000-0000-000000000002', 2, 'Gabinete de Medios', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Estudio de grabación, edición y streaming para prácticas audiovisuales.', 2012, 60.0, 15, '08 a 22 hs', 'Licenciatura en Comunicación Social'),

    ('66600001-0000-0000-0000-000000000003', 3, 'Laboratorio de Alta Complejidad (LAC)', 'INTERNO', 'Laboratorio', 'Nacional',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Equipamiento para análisis de agua, suelo, alimentos y microbiología. 4 laboratorios de 59 m2 y dos de 37 m2.', 2014, 642.0, 25, '08 a 18 hs', 'Ing. Agronómica, Lic. en Biología'),

    ('66600001-0000-0000-0000-000000000004', 4, 'Laboratorio de Estudios Regionales (LER)', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', '8 salas comunes para I+D+i, análisis de muestras, microvinificación, microscopía.', 2019, 142.0, 20, '08 a 20 hs', 'Lic. en Biología, Cs. Naturales'),

    ('66600001-0000-0000-0000-000000000005', 5, 'Laboratorio de Física', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Equipamiento para ensayos de cinemática, óptica, termodinámica y electricidad.', 2010, 55.0, 25, '08 a 22 hs', 'Ingeniería en Agrimensura, Mecatrónica'),

    ('66600001-0000-0000-0000-000000000006', 6, 'Laboratorio de Informática A', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Espacio de 6x12 m (72 m2). 24 PCs en red LAN, enlace simétrico, software pedagógico Faronics Insight.', 2007, 72.0, 45, '08 a 22 hs', 'Ingeniería en Agrimensura, Sistemas'),

    ('66600001-0000-0000-0000-000000000007', 7, 'Laboratorio de Informática B', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Espacio de 6x12 m (72 m2). 20 PCs en red LAN, enlace digital interactivo.', 2007, 72.0, 45, '08 a 22 hs', 'Ingeniería en Agrimensura, Sistemas'),

    ('66600001-0000-0000-0000-000000000008', 8, 'Laboratorio de Mecatrónica', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Mesas de trabajo, osciloscopios digitales, servidores SPARC, impresoras 3D y bancos de prueba.', 2015, 38.0, 15, '08 a 22 hs', 'Ingeniería Mecatrónica'),

    ('66600001-0000-0000-0000-000000000009', 9, 'Laboratorio de Microscopía', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Nueve mesadas antisísmicas con dos microscopios ópticos cada una y proyección vertical.', 2006, 49.0, 20, '08 a 22 hs', 'Lic. en Biología, Enfermería'),

    ('66600001-0000-0000-0000-000000000010', 10, 'Laboratorio de Química 1', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Mesadas de granito, campana de extracción de gases, piletas y material de vidrio volumétrico.', 2007, 49.0, 25, '08 a 22 hs', 'Ingeniería Agronómica, Agrimensura'),

    ('66600001-0000-0000-0000-000000000011', 11, 'Laboratorio de Química 2', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Mesadas laterales y central de granito, campana extractora y reactivos.', 2010, 50.0, 25, '08 a 22 hs', 'Ingeniería Agronómica, Enología'),

    ('66600001-0000-0000-0000-000000000012', 12, 'Laboratorio de Redes', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Racks con routers y switches Cisco para prácticas de topología y direccionamiento IP.', 2015, 36.0, 15, '08 a 22 hs', 'Sistemas, Mecatrónica'),

    ('66600001-0000-0000-0000-000000000013', 13, 'Laboratorio de Software Libre', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Equipos dedicados a desarrollo bajo entornos GNU/Linux y tecnologías abiertas.', 2005, 32.0, 15, '08 a 22 hs', 'Sistemas'),

    ('66600001-0000-0000-0000-000000000014', 14, 'Laboratorio de Sistemas Operativos (ex SUN)', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Terminales de alta velocidad y servidores para entornos concurrentes.', 2005, 32.0, 15, '08 a 22 hs', 'Sistemas'),

    ('66600001-0000-0000-0000-000000000015', 15, 'Laboratorio de Investigación (Sistemas)', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Espacio para proyectos de investigación de docentes e ingenieros graduados.', 2015, 35.0, 12, '08 a 22 hs', 'Sistemas'),

    ('66600001-0000-0000-0000-000000000016', 16, 'Laboratorio PROGRESAR', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Sala de cómputo para acompañamiento tutorial y estudio extracurricular de alumnos.', 2005, 32.0, 15, '08 a 22 hs', 'Todas las carreras'),

    ('66600001-0000-0000-0000-000000000017', 17, 'Sala de Audiencia y Cámara Gesell', 'INTERNO', 'Centro Judicial', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Ambiente judicial simulado con vidrio unidireccional y audio estereofónico para litigación.', 2008, 36.0, 20, '08 a 22 hs', 'Abogacía'),

    ('66600001-0000-0000-0000-000000000018', 18, 'Laboratorio de Energías Renovables', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Banco de pruebas solares, fotovoltaicas y eólicas.', 2018, 40.0, 15, '08 a 22 hs', 'Ingeniería Mecatrónica'),

    ('66600001-0000-0000-0000-000000000019', 19, 'Laboratorio de Programación y Bases de Datos', 'INTERNO', 'Laboratorio', 'Universitario',
     v_inmueble_sarmientos_id, 'Campus Los Sarmientos', 'Servidores SunRay, switches Catalyst y routers Cisco para modelos relacionales y no relacionales.', 2005, 36.0, 10, '08 a 22 hs', 'Sistemas, Agrimensura')
    ON CONFLICT (id) DO NOTHING;

    -- Unidades Demostrativas / Experimentales (20 a 22)
    INSERT INTO ambito_practica (
        id, numero_orden, denominacion, tipo_categoria, tipo_ambito, tipo_establecimiento,
        inmueble_id, inmueble_nombre_referencia, descripcion, superficie_m2, capacidad_alumnos,
        disponibilidad_horaria, actividades_demostrativas, carreras_que_lo_usan
    ) VALUES
    ('66600001-0000-0000-0000-000000000020', 20, 'Campo Experimental y Productivo (200 ha)', 'UNIDAD_DEMOSTRATIVA', 'Campo experimental y productivo', 'Universitario',
     v_inmueble_tilimuqui_id, 'Campo Tilimuqui', 'Campo de 200 ha con vegetación natural, perforación de 170 m y galpón auxiliar.', 2000000.0, 100, '07 a 20 hs', 'Topografía de campo, agrimensura rural, geodesia y suelos.', 'Ingeniería en Agrimensura, Agronomía'),

    ('66600001-0000-0000-0000-000000000021', 21, 'Campo Experimental y Productivo (50 ha)', 'UNIDAD_DEMOSTRATIVA', 'Campo experimental y productivo', 'Universitario',
     v_inmueble_tilimuqui_id, 'Campo Tilimuqui', 'Campo de 50 ha alambrado con perforación de 170 m para ensayos agrícolas.', 500000.0, 50, '07 a 20 hs', 'Mediciones especiales y ensayos de riego.', 'Ingeniería Agronómica'),

    ('66600001-0000-0000-0000-000000000022', 22, 'Parcela Didáctica y Productiva (15 ha)', 'UNIDAD_DEMOSTRATIVA', 'Campo experimental', 'Universitario',
     v_inmueble_tilimuqui_id, 'Campo Tilimuqui', 'Parcela de 15 ha con cultivos y producción de granja: galpón avícola, gallinero (88 gallinas), corrales (25 cabras), conejeras (92 conejos).', 150000.0, 30, '07 a 20 hs', 'Manejo animal, avicultura, producción forrajera.', 'Ingeniería Agronómica, Biología')
    ON CONFLICT (id) DO NOTHING;

    -- Centros de Atención Médica (CAM) vinculados con Convenios FK
    INSERT INTO ambito_practica (
        id, numero_orden, denominacion, tipo_categoria, tipo_ambito, tipo_establecimiento,
        convenio_id, direccion, localidad, provincia, departamento, codigo_postal,
        tipo_dependencia, sector, actividad_alumno_cam, carreras_que_lo_usan
    ) VALUES
    ('66600001-0000-0000-0000-000000000031', 31, 'Clínica San Nicolás', 'CAM', 'Centro de Salud', 'Privado',
     c_clinica_san_nicolas_id, 'La Plata N° 240', 'Chilecito', 'La Rioja', 'Chilecito', '5360',
     'Centro de Salud', 'Privado', 'Cuidados de enfermería brindados a problemas respiratorios, cardíaco, digestivo y shock.', 'Licenciatura en Enfermería'),

    ('66600001-0000-0000-0000-000000000032', 32, 'Hospital El Cruce Alta Complejidad en Red', 'CAM', 'Hospital', 'Provincial',
     c_hospital_el_cruce_id, 'Avenida Calchaquí N° 5401', 'Florencio Varela', 'Buenos Aires', 'Florencio Varela', '1888',
     'Hospital', 'Público provincial', 'Reconocimiento de servicios de cuidados intensivos, monitores multiparamétricos, respiradores y soporte metabólico.', 'Licenciatura en Enfermería'),

    ('66600001-0000-0000-0000-000000000033', 33, 'Hospital de la Madre y el Niño', 'CAM', 'Hospital', 'Nacional',
     c_hospital_madre_nino_id, '1ero de Marzo S/N', 'La Rioja', 'La Rioja', 'Capital', '5300',
     'Hospital', 'Público nacional', 'Cuidados a la madre y neonatos en sectores básicos, intermedios e intensivos.', 'Licenciatura en Enfermería'),

    ('66600001-0000-0000-0000-000000000034', 34, 'Hospital Distrital Ramón Carrillo', 'CAM', 'Hospital', 'Provincial',
     c_hospital_carrillo_id, 'Avenida San Pedro S/N', 'Famatina', 'La Rioja', 'Famatina', '5365',
     'Hospital', 'Público provincial', 'Atención en consultorio y guardia, internación clínica médica y atención materno-infantil rural.', 'Licenciatura en Enfermería'),

    ('66600001-0000-0000-0000-000000000035', 35, 'Hospital Dr. Arturo Oñativia', 'CAM', 'Hospital', 'Provincial',
     c_hospital_onativia_id, 'Dr. Ramón Carrillo N° 1339', 'Rafael Calzada', 'Buenos Aires', 'Almirante Brown', '1847',
     'Hospital', 'Público provincial', 'Cuidados intensivos y emergencia de alta complejidad respiratoria y cardiovascular.', 'Licenciatura en Enfermería'),

    ('66600001-0000-0000-0000-000000000036', 36, 'Hospital Eleazar Herrera Motta', 'CAM', 'Hospital', 'Nacional',
     c_hospital_motta_id, 'Gregorio Cavero N° 500', 'Chilecito', 'La Rioja', 'Chilecito', '5360',
     'Hospital', 'Público nacional', 'Rotación por clínica médica, quirúrgica, traumatología, obstetricia, pediatría y UTI pre, intra y postoperatorio.', 'Licenciatura en Enfermería'),

    ('66600001-0000-0000-0000-000000000037', 37, 'Hospital Regional Dr. Enrique Vera Barros', 'CAM', 'Hospital', 'Nacional',
     c_hospital_vera_barros_id, 'Rivadavia N° 533', 'La Rioja', 'La Rioja', 'Capital', '5300',
     'Hospital', 'Público nacional', 'Cuidados de adultos y ancianos en guardia, venoclisis, bombas de perfusión y terapia intensiva.', 'Licenciatura en Enfermería'),

    ('66600001-0000-0000-0000-000000000038', 38, 'Zona Región Sanitaria III', 'CAM', 'Organización Social', 'Provincial',
     c_zona_sanitaria3_id, 'Av. Circunvalación S/N', 'Chilecito', 'La Rioja', 'Chilecito', '5360',
     'Organización Social', 'Público provincial', 'Promoción de la salud comunitaria, inmunización y prevención en territorio.', 'Licenciatura en Enfermería'),

    ('66600001-0000-0000-0000-000000000039', 39, 'Centro Terapéutico de Adicciones MUNAY', 'CAM', 'Organización Social', 'Provincial',
     c_munay_id, 'Hunicken S/N San Lorenzo', 'Chilecito', 'La Rioja', 'Chilecito', '5360',
     'Organización Social', 'Público provincial', 'Orientación y tratamiento de personas con consumos problemáticos de sustancias.', 'Licenciatura en Enfermería'),

    ('66600001-0000-0000-0000-000000000040', 40, 'Instituto Nacional de Servicios Sociales para Jubilados (PAMI)', 'CAM', 'Organización Social', 'Nacional',
     c_pami_id, 'Avda. Facundo Quiroga', 'Chilecito', 'La Rioja', 'Chilecito', '5360',
     'Organización Social', 'Público Nacional', 'Contención de adultos mayores en acciones que contribuyen a la salud y bienestar psicofísico.', 'Licenciatura en Enfermería'),

    ('66600001-0000-0000-0000-000000000041', 41, 'Instituto Universitario de Ciencias Biomédicas de Córdoba', 'CAM', 'Organización Social', 'Nacional',
     c_iucbc_id, 'Naciones Unidas Bº Parque Vélez Sarsfield', 'Córdoba', 'Córdoba', 'Capital', '5000',
     'Organización Social', 'Público Nacional', 'Colaboración y vinculación estratégica en docencia e investigación clínica.', 'Licenciatura en Enfermería')
    ON CONFLICT (id) DO NOTHING;

    -- 3. SEED DE PLAN DE ESTUDIOS FT-001 (Ingeniería en Agrimensura)
    SELECT id INTO v_carrera_agrim_id FROM carrera WHERE sigla = 'IAGRIM' LIMIT 1;
    IF v_carrera_agrim_id IS NOT NULL THEN
        INSERT INTO plan_estudio_ft001 (
            id, carrera_id, codigo_resolucion, anio_aprobacion, anio_inicio, duracion_anios,
            horas_presenciales, horas_no_presenciales, horas_totales, horas_optativas_minimas,
            estructura_sintetica, instancia_final_graduacion, activo
        ) VALUES (
            v_plan_agrim_id,
            v_carrera_agrim_id,
            'Ordenanza HCS 015/24',
            2024,
            2025,
            5,
            3650,
            0,
            3650,
            0,
            'El plan de estudio Nro 015/24 de la carrera Ingeniería en Agrimensura tiene una duración de 5 años estructurados en 10 cuatrimestres, con una duración total de 3650 horas reloj (incluidos Trabajo Final de 180 horas y 200 horas de PPS). Posee 46 asignaturas obligatorias organizadas en cuatro ciclos: Ciencias Básicas de la Ingeniería (C.B.I., 975 hs), Tecnologías Básicas (T.B., 555 hs), Tecnologías Aplicadas (T.A., 1230 hs) y Ciencias y Tecnologías Complementarias (C.T.C., 690 hs).',
            'Práctica Profesional Supervisada (PPS) de 200 horas reloj (al superar 75% de las asignaturas) en dependencias de la UNdeC o entidades públicas/privadas con convenio marco o protocolo específico. Trabajo Final con características de asignatura (180 hs reloj) en modalidad de campo o investigación, con director docente designado y defensa ante Tribunal Examinador.',
            TRUE
        ) ON CONFLICT (id) DO NOTHING;

        -- 4. SEED DE LAS 46 ASIGNATURAS DE INGENIERÍA EN AGRIMENSURA
        INSERT INTO actividad_curricular_plan (id, plan_estudio_id, numero_orden, nombre, grupo, subgrupo, horas_reloj, correlativas) VALUES
        (gen_random_uuid(), v_plan_agrim_id, 1, 'Análisis Matemático I', 'C.B.I.', '1º', 105, ''),
        (gen_random_uuid(), v_plan_agrim_id, 2, 'Álgebra I', 'C.B.I.', '1º', 105, ''),
        (gen_random_uuid(), v_plan_agrim_id, 3, 'Sistemas de Representación', 'C.B.I.', '1º', 90, ''),
        (gen_random_uuid(), v_plan_agrim_id, 4, 'Introducción a la Agrimensura', 'C.T.C.', '1º', 75, ''),
        (gen_random_uuid(), v_plan_agrim_id, 5, 'Análisis Matemático II', 'C.B.I.', '1º', 105, 'Análisis Matemático I'),
        (gen_random_uuid(), v_plan_agrim_id, 6, 'Fundamentos de Informática', 'C.B.I.', '1º', 75, 'Álgebra I'),
        (gen_random_uuid(), v_plan_agrim_id, 7, 'Álgebra II', 'C.B.I.', '1º', 90, 'Álgebra I'),
        (gen_random_uuid(), v_plan_agrim_id, 8, 'Geometría Analítica', 'C.B.I.', '1º', 90, 'Álgebra I'),
        (gen_random_uuid(), v_plan_agrim_id, 9, 'Trigonometría', 'C.B.I.', '1º', 75, ''),
        (gen_random_uuid(), v_plan_agrim_id, 10, 'Análisis Matemático III', 'C.B.I.', '2º', 105, 'Análisis Matemático I'),
        (gen_random_uuid(), v_plan_agrim_id, 11, 'Física I', 'C.B.I.', '2º', 105, ''),
        (gen_random_uuid(), v_plan_agrim_id, 12, 'Informática', 'T.B.', '2º', 75, 'Fundamentos de Informática'),
        (gen_random_uuid(), v_plan_agrim_id, 13, 'Probabilidad y Estadística', 'C.B.I.', '2º', 75, 'Análisis Matemático II, Álgebra II'),
        (gen_random_uuid(), v_plan_agrim_id, 14, 'Topografía I', 'T.B.', '2º', 120, 'Sistemas de Representación, Trigonometría'),
        (gen_random_uuid(), v_plan_agrim_id, 15, 'Matemáticas Especiales', 'C.B.I.', '2º', 75, 'Análisis Matemático III'),
        (gen_random_uuid(), v_plan_agrim_id, 16, 'Física II', 'C.B.I.', '2º', 105, 'Física I'),
        (gen_random_uuid(), v_plan_agrim_id, 17, 'Topografía II', 'T.A.', '2º', 120, 'Topografía I'),
        (gen_random_uuid(), v_plan_agrim_id, 18, 'Dibujo Topográfico y Cartográfico', 'T.B.', '2º', 75, 'Topografía I'),
        (gen_random_uuid(), v_plan_agrim_id, 19, 'Geografía Física y Geomorfología', 'C.T.C.', '2º', 60, 'Topografía I'),
        (gen_random_uuid(), v_plan_agrim_id, 20, 'Introducción al Derecho', 'T.B.', '3º', 60, 'Introducción a la Agrimensura'),
        (gen_random_uuid(), v_plan_agrim_id, 21, 'Topografía III', 'T.A.', '3º', 120, 'Topografía II'),
        (gen_random_uuid(), v_plan_agrim_id, 22, 'Cálculo de Compensación', 'T.B.', '3º', 75, 'Matemáticas Especiales, Topografía II'),
        (gen_random_uuid(), v_plan_agrim_id, 23, 'Economía y Gestión Empresarial', 'C.T.C.', '3º', 75, 'Análisis Matemático II, Álgebra II'),
        (gen_random_uuid(), v_plan_agrim_id, 24, 'Cartografía', 'T.B.', '3º', 90, 'Dibujo Topográfico y Cartográfico'),
        (gen_random_uuid(), v_plan_agrim_id, 25, 'Teledetección Satelital', 'T.A.', '3º', 90, 'Dibujo Topográfico y Cartográfico, Geografía Física y Geomorfología'),
        (gen_random_uuid(), v_plan_agrim_id, 26, 'Geodesia Física y Geométrica', 'T.A.', '3º', 105, 'Cartografía'),
        (gen_random_uuid(), v_plan_agrim_id, 27, 'Formulación y Evaluación de Proyectos. Organización Industrial', 'C.T.C.', '3º', 75, 'Economía y Gestión Empresarial'),
        (gen_random_uuid(), v_plan_agrim_id, 28, 'Derechos Reales y Registrales', 'T.B.', '3º', 75, 'Introducción al Derecho'),
        (gen_random_uuid(), v_plan_agrim_id, 29, 'Fotogrametría y Fotointerpretación', 'T.A.', '4º', 105, 'Teledetección Satelital'),
        (gen_random_uuid(), v_plan_agrim_id, 30, 'Geodesia Espacial', 'T.A.', '4º', 90, 'Geodesia Física y Geométrica'),
        (gen_random_uuid(), v_plan_agrim_id, 31, 'Gestión Ambiental', 'C.T.C.', '4º', 60, 'Geografía Física y Geomorfología'),
        (gen_random_uuid(), v_plan_agrim_id, 32, 'Sistemas de Información Territorial', 'T.A.', '4º', 90, 'Cartografía, Teledetección Satelital'),
        (gen_random_uuid(), v_plan_agrim_id, 33, 'Información Rural y Agrología', 'C.T.C.', '4º', 75, 'Geografía Física y Geomorfología'),
        (gen_random_uuid(), v_plan_agrim_id, 34, 'Elementos de Edificios', 'C.T.C.', '4º', 60, 'Dibujo Topográfico y Cartográfico'),
        (gen_random_uuid(), v_plan_agrim_id, 35, 'Catastro Territorial', 'T.A.', '4º', 105, 'Fotogrametría y Fotointerpretación, Sistemas de Información Territorial, Elementos de Edificios'),
        (gen_random_uuid(), v_plan_agrim_id, 36, 'Valuaciones Inmobiliarias', 'T.A.', '4º', 90, 'Información Rural y Agrología, Elementos de Edificios'),
        (gen_random_uuid(), v_plan_agrim_id, 37, 'Agrimensura Legal', 'T.A.', '4º', 90, 'Derechos Reales y Registrales'),
        (gen_random_uuid(), v_plan_agrim_id, 38, 'Inglés', 'C.T.C.', '4º', 60, ''),
        (gen_random_uuid(), v_plan_agrim_id, 39, 'Mensura', 'T.A.', '5º', 120, 'Agrimensura Legal'),
        (gen_random_uuid(), v_plan_agrim_id, 40, 'Ordenamiento Territorial y Planeamiento', 'C.T.C.', '5º', 75, 'Catastro Territorial'),
        (gen_random_uuid(), v_plan_agrim_id, 41, 'Mediciones Especiales', 'C.T.C.', '5º', 75, 'Geodesia Espacial'),
        (gen_random_uuid(), v_plan_agrim_id, 42, 'Seguridad, Higiene y Medio Ambiente', 'T.A.', '5º', 60, 'Gestión Ambiental'),
        (gen_random_uuid(), v_plan_agrim_id, 43, 'Cartografía Digital', 'T.A.', '5º', 75, 'Sistemas de Información Territorial'),
        (gen_random_uuid(), v_plan_agrim_id, 44, 'Hidráulica Aplicada', 'T.A.', '5º', 75, 'Mediciones Especiales'),
        (gen_random_uuid(), v_plan_agrim_id, 45, 'Vías de Comunicación', 'T.A.', '5º', 75, 'Mediciones Especiales'),
        (gen_random_uuid(), v_plan_agrim_id, 46, 'Trabajo Final', 'T.A.', '5º', 180, 'Todas las asignaturas previas del plan aprobadas');
    END IF;
END $$;
