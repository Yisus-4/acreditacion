package com.undec.acreditacion.infrastructure.config;

import com.undec.acreditacion.application.input.ChangeUserStatusUseCase;
import com.undec.acreditacion.application.input.CreateUserUseCase;
import com.undec.acreditacion.application.input.ListRolesUseCase;
import com.undec.acreditacion.application.input.ListUsersUseCase;
import com.undec.acreditacion.application.input.LoginUseCase;
import com.undec.acreditacion.application.input.RegisterUserUseCase;
import com.undec.acreditacion.application.input.UpdateUserUseCase;
import com.undec.acreditacion.application.output.PasswordHasher;
import com.undec.acreditacion.application.output.RoleRepository;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.application.usecase.ChangeUserStatusService;
import com.undec.acreditacion.application.usecase.CreateUserService;
import com.undec.acreditacion.application.usecase.ListRolesService;
import com.undec.acreditacion.application.usecase.ListUsersService;
import com.undec.acreditacion.application.usecase.LoginService;
import com.undec.acreditacion.application.usecase.RegisterUserService;
import com.undec.acreditacion.application.usecase.UpdateUserService;
import com.undec.acreditacion.application.input.ConsultarGeografiaUseCase;
import com.undec.acreditacion.application.input.ActualizarInstitucionUseCase;
import com.undec.acreditacion.application.input.CambiarEstadoInstitucionUseCase;
import com.undec.acreditacion.application.input.ConsultarInstitucionUseCase;
import com.undec.acreditacion.application.input.EliminarInstitucionUseCase;
import com.undec.acreditacion.application.usecase.ActualizarInstitucionService;
import com.undec.acreditacion.application.usecase.CambiarEstadoInstitucionService;
import com.undec.acreditacion.application.usecase.ConsultarInstitucionService;
import com.undec.acreditacion.application.usecase.EliminarInstitucionService;
import com.undec.acreditacion.application.input.ListarInstitucionesUseCase;
import com.undec.acreditacion.application.input.RegistrarInstitucionUseCase;
import com.undec.acreditacion.application.usecase.ConsultarGeografiaService;
import com.undec.acreditacion.application.usecase.ListarInstitucionesService;
import com.undec.acreditacion.application.usecase.RegistrarInstitucionService;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.GeografiaRepositoryPort;
import com.undec.acreditacion.domain.ports.InstitucionRepositoryPort;
import com.undec.acreditacion.application.input.IniciarConvocatoriaUseCase;
import com.undec.acreditacion.application.input.ConsultarConvocatoriaUseCase;
import com.undec.acreditacion.application.input.ListarConvocatoriasUseCase;
import com.undec.acreditacion.application.input.EliminarConvocatoriaUseCase;
import com.undec.acreditacion.application.input.ConsultarPlanTrabajoUseCase;
import com.undec.acreditacion.application.input.CompletarActividadInformarProcesoUseCase;
import com.undec.acreditacion.application.input.DescargarDocumentoUseCase;
import com.undec.acreditacion.application.input.ElaborarPlanTrabajoUseCase;
import com.undec.acreditacion.application.input.ObtenerPlantillaPlanTrabajoUseCase;
import com.undec.acreditacion.application.usecase.IniciarConvocatoriaService;
import com.undec.acreditacion.application.usecase.ConsultarConvocatoriaService;
import com.undec.acreditacion.application.usecase.ListarConvocatoriasService;
import com.undec.acreditacion.application.usecase.EliminarConvocatoriaService;
import com.undec.acreditacion.application.usecase.ConsultarPlanTrabajoService;
import com.undec.acreditacion.application.usecase.CompletarActividadInformarProcesoService;
import com.undec.acreditacion.application.usecase.DescargarDocumentoService;
import com.undec.acreditacion.application.usecase.ElaborarPlanTrabajoService;
import com.undec.acreditacion.application.usecase.ObtenerPlantillaPlanTrabajoService;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConvocatoriaRepositoryPort;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.domain.ports.DocumentoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.CarreraRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wires pure (framework-agnostic) application services to their input ports.
 *
 * <p>The application layer ({@link LoginService}) intentionally carries no
 * Spring annotations; this infrastructure configuration is the single place
 * that knows how to assemble and expose it as a Spring bean.</p>
 */
@Configuration
public class UseCaseConfiguration {

    @Bean
    public LoginUseCase loginUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        return new LoginService(userRepository, passwordHasher);
    }

    @Bean
    public RegisterUserUseCase registerUserUseCase(UserRepository userRepository) {
        return new RegisterUserService(userRepository);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserRepository userRepository) {
        return new ListUsersService(userRepository);
    }

    @Bean
    public ListRolesUseCase listRolesUseCase(RoleRepository roleRepository) {
        return new ListRolesService(roleRepository);
    }

    @Bean
    public ChangeUserStatusUseCase changeUserStatusUseCase(UserRepository userRepository) {
        return new ChangeUserStatusService(userRepository);
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase(UserRepository userRepository,
                                             RoleRepository roleRepository,
                                             PasswordHasher passwordHasher) {
        return new UpdateUserService(userRepository, roleRepository, passwordHasher);
    }

    @Bean
    public CreateUserUseCase createUserUseCase(UserRepository userRepository,
                                             RoleRepository roleRepository,
                                             PasswordHasher passwordHasher) {
        return new CreateUserService(userRepository, roleRepository, passwordHasher);
    }

    @Bean
    public RegistrarInstitucionUseCase registrarInstitucionUseCase(InstitucionRepositoryPort institucionRepository,
                                                                   DocumentStoragePort documentStoragePort) {
        return new RegistrarInstitucionService(institucionRepository, documentStoragePort);
    }

    @Bean
    public ListarInstitucionesUseCase listarInstitucionesUseCase(InstitucionRepositoryPort institucionRepository) {
        return new ListarInstitucionesService(institucionRepository);
    }

    @Bean
    @Transactional
    public ConsultarInstitucionUseCase consultarInstitucionUseCase(InstitucionRepositoryPort institucionRepository) {
        return new ConsultarInstitucionService(institucionRepository);
    }

    @Bean
    @Transactional
    public ActualizarInstitucionUseCase actualizarInstitucionUseCase(InstitucionRepositoryPort institucionRepository,
                                                                     DocumentStoragePort documentStoragePort) {
        return new ActualizarInstitucionService(institucionRepository, documentStoragePort);
    }

    @Bean
    @Transactional
    public CambiarEstadoInstitucionUseCase cambiarEstadoInstitucionUseCase(InstitucionRepositoryPort institucionRepository) {
        return new CambiarEstadoInstitucionService(institucionRepository);
    }

    @Bean
    @Transactional
    public EliminarInstitucionUseCase eliminarInstitucionUseCase(InstitucionRepositoryPort institucionRepository) {
        return new EliminarInstitucionService(institucionRepository);
    }

    @Bean
    public ConsultarGeografiaUseCase consultarGeografiaUseCase(GeografiaRepositoryPort geografiaRepository) {
        return new ConsultarGeografiaService(geografiaRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.RegistrarUnidadAcademicaUseCase registrarUnidadAcademicaUseCase(
            com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort unidadAcademicaRepository,
            InstitucionRepositoryPort institucionRepository) {
        return new com.undec.acreditacion.application.usecase.RegistrarUnidadAcademicaService(unidadAcademicaRepository, institucionRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.ListarUnidadesAcademicasUseCase listarUnidadesAcademicasUseCase(
            com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort unidadAcademicaRepository) {
        return new com.undec.acreditacion.application.usecase.ListarUnidadesAcademicasService(unidadAcademicaRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.RegistrarSedeLocalizacionUseCase registrarSedeLocalizacionUseCase(
            com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort sedeLocalizacionRepository,
            InstitucionRepositoryPort institucionRepository) {
        return new com.undec.acreditacion.application.usecase.RegistrarSedeLocalizacionService(sedeLocalizacionRepository, institucionRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.ListarSedesLocalizacionesUseCase listarSedesLocalizacionesUseCase(
            com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort sedeLocalizacionRepository) {
        return new com.undec.acreditacion.application.usecase.ListarSedesLocalizacionesService(sedeLocalizacionRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.RegistrarCarreraUseCase registrarCarreraUseCase(
            com.undec.acreditacion.domain.ports.CarreraRepositoryPort carreraRepository,
            com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort unidadAcademicaRepository,
            com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort sedeLocalizacionRepository) {
        return new com.undec.acreditacion.application.usecase.RegistrarCarreraService(carreraRepository, unidadAcademicaRepository, sedeLocalizacionRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.ListarCarrerasUseCase listarCarrerasUseCase(
            com.undec.acreditacion.domain.ports.CarreraRepositoryPort carreraRepository) {
        return new com.undec.acreditacion.application.usecase.ListarCarrerasService(carreraRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.ActualizarUnidadAcademicaUseCase actualizarUnidadAcademicaUseCase(
            com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort unidadAcademicaRepository) {
        return new com.undec.acreditacion.application.usecase.ActualizarUnidadAcademicaService(unidadAcademicaRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.CambiarEstadoUnidadAcademicaUseCase cambiarEstadoUnidadAcademicaUseCase(
            com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort unidadAcademicaRepository) {
        return new com.undec.acreditacion.application.usecase.CambiarEstadoUnidadAcademicaService(unidadAcademicaRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.EliminarUnidadAcademicaUseCase eliminarUnidadAcademicaUseCase(
            com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort unidadAcademicaRepository,
            com.undec.acreditacion.domain.ports.CarreraRepositoryPort carreraRepository) {
        return new com.undec.acreditacion.application.usecase.EliminarUnidadAcademicaService(unidadAcademicaRepository, carreraRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.ActualizarSedeLocalizacionUseCase actualizarSedeLocalizacionUseCase(
            com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort sedeLocalizacionRepository) {
        return new com.undec.acreditacion.application.usecase.ActualizarSedeLocalizacionService(sedeLocalizacionRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.CambiarEstadoSedeLocalizacionUseCase cambiarEstadoSedeLocalizacionUseCase(
            com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort sedeLocalizacionRepository) {
        return new com.undec.acreditacion.application.usecase.CambiarEstadoSedeLocalizacionService(sedeLocalizacionRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.EliminarSedeLocalizacionUseCase eliminarSedeLocalizacionUseCase(
            com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort sedeLocalizacionRepository,
            com.undec.acreditacion.domain.ports.CarreraRepositoryPort carreraRepository) {
        return new com.undec.acreditacion.application.usecase.EliminarSedeLocalizacionService(sedeLocalizacionRepository, carreraRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.ActualizarCarreraUseCase actualizarCarreraUseCase(
            com.undec.acreditacion.domain.ports.CarreraRepositoryPort carreraRepository,
            com.undec.acreditacion.domain.ports.SedeLocalizacionRepositoryPort sedeLocalizacionRepository) {
        return new com.undec.acreditacion.application.usecase.ActualizarCarreraService(carreraRepository, sedeLocalizacionRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.CambiarEstadoCarreraUseCase cambiarEstadoCarreraUseCase(
            com.undec.acreditacion.domain.ports.CarreraRepositoryPort carreraRepository) {
        return new com.undec.acreditacion.application.usecase.CambiarEstadoCarreraService(carreraRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.EliminarCarreraUseCase eliminarCarreraUseCase(
            com.undec.acreditacion.domain.ports.CarreraRepositoryPort carreraRepository) {
        return new com.undec.acreditacion.application.usecase.EliminarCarreraService(carreraRepository);
    }

    @Bean
    @Transactional
    public IniciarConvocatoriaUseCase iniciarConvocatoriaUseCase(
            ProcesoAcreditacionRepositoryPort procesoRepository,
            RegistroConvocatoriaRepositoryPort registroRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            CarreraRepositoryPort carreraRepository) {
        return new IniciarConvocatoriaService(procesoRepository, registroRepository, planTrabajoRepository, carreraRepository);
    }

    @Bean
    @Transactional
    public CompletarActividadInformarProcesoUseCase completarActividadInformarProcesoUseCase(
            PlanTrabajoRepositoryPort planTrabajoRepository,
            DocumentoAcreditacionRepositoryPort documentoRepository,
            DocumentStoragePort documentStoragePort) {
        return new CompletarActividadInformarProcesoService(planTrabajoRepository, documentoRepository, documentStoragePort);
    }

    @Bean
    public ConsultarConvocatoriaUseCase consultarConvocatoriaUseCase(ProcesoAcreditacionRepositoryPort procesoRepository) {
        return new ConsultarConvocatoriaService(procesoRepository);
    }

    @Bean
    public ListarConvocatoriasUseCase listarConvocatoriasUseCase(ProcesoAcreditacionRepositoryPort procesoRepository) {
        return new ListarConvocatoriasService(procesoRepository);
    }

    @Bean
    @Transactional
    public EliminarConvocatoriaUseCase eliminarConvocatoriaUseCase(ProcesoAcreditacionRepositoryPort procesoRepository) {
        return new EliminarConvocatoriaService(procesoRepository);
    }

    @Bean
    public ConsultarPlanTrabajoUseCase consultarPlanTrabajoUseCase(PlanTrabajoRepositoryPort planTrabajoRepository) {
        return new ConsultarPlanTrabajoService(planTrabajoRepository);
    }

    @Bean
    public DescargarDocumentoUseCase descargarDocumentoUseCase(
            DocumentoAcreditacionRepositoryPort documentoRepository,
            DocumentStoragePort documentStoragePort) {
        return new DescargarDocumentoService(documentoRepository, documentStoragePort);
    }

    @Bean
    @Transactional
    public ElaborarPlanTrabajoUseCase elaborarPlanTrabajoUseCase(
            ProcesoAcreditacionRepositoryPort procesoRepository,
            RegistroConvocatoriaRepositoryPort registroRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository) {
        return new ElaborarPlanTrabajoService(procesoRepository, registroRepository, planTrabajoRepository);
    }

    @Bean
    public ObtenerPlantillaPlanTrabajoUseCase obtenerPlantillaPlanTrabajoUseCase(
            PlanTrabajoRepositoryPort planTrabajoRepository) {
        return new ObtenerPlantillaPlanTrabajoService(planTrabajoRepository);
    }

    @Bean
    @Transactional
    public com.undec.acreditacion.application.input.SolicitarNormativaUseCase solicitarNormativaUseCase(
            ProcesoAcreditacionRepositoryPort procesoRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            com.undec.acreditacion.domain.ports.SolicitudNormativaRepositoryPort solicitudRepository,
            com.undec.acreditacion.domain.ports.DocumentoNormativaRepositoryPort documentoRepository,
            CarreraRepositoryPort carreraRepository,
            RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository,
            com.undec.acreditacion.domain.ports.EmailNotificationPort emailNotificationPort) {
        return new com.undec.acreditacion.application.usecase.SolicitarNormativaService(
                procesoRepository, planTrabajoRepository, solicitudRepository,
                documentoRepository, carreraRepository, registroConvocatoriaRepository, emailNotificationPort);
    }

    @Bean
    public com.undec.acreditacion.application.input.ObtenerNormativaDetalleUseCase obtenerNormativaDetalleUseCase(
            ProcesoAcreditacionRepositoryPort procesoRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            com.undec.acreditacion.domain.ports.SolicitudNormativaRepositoryPort solicitudRepository,
            com.undec.acreditacion.domain.ports.DocumentoNormativaRepositoryPort documentoRepository) {
        return new com.undec.acreditacion.application.usecase.ObtenerNormativaDetalleService(
                procesoRepository, planTrabajoRepository, solicitudRepository, documentoRepository);
    }

    @Bean
    @Transactional
    public com.undec.acreditacion.application.input.GuardarDocumentoBorradorUseCase guardarDocumentoBorradorUseCase(
            ProcesoAcreditacionRepositoryPort procesoRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            com.undec.acreditacion.domain.ports.SolicitudNormativaRepositoryPort solicitudRepository,
            com.undec.acreditacion.domain.ports.DocumentoNormativaRepositoryPort documentoRepository,
            DocumentStoragePort documentStoragePort,
            CarreraRepositoryPort carreraRepository,
            RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository) {
        return new com.undec.acreditacion.application.usecase.GuardarDocumentoBorradorService(
                procesoRepository, planTrabajoRepository, solicitudRepository, documentoRepository,
                documentStoragePort, carreraRepository, registroConvocatoriaRepository);
    }

    @Bean
    @Transactional
    public com.undec.acreditacion.application.input.EliminarDocumentoBorradorUseCase eliminarDocumentoBorradorUseCase(
            ProcesoAcreditacionRepositoryPort procesoRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            com.undec.acreditacion.domain.ports.SolicitudNormativaRepositoryPort solicitudRepository,
            com.undec.acreditacion.domain.ports.DocumentoNormativaRepositoryPort documentoRepository,
            DocumentStoragePort documentStoragePort) {
        return new com.undec.acreditacion.application.usecase.EliminarDocumentoBorradorService(
                procesoRepository, planTrabajoRepository, solicitudRepository, documentoRepository, documentStoragePort);
    }

    @Bean
    @Transactional
    public com.undec.acreditacion.application.input.EnviarNormativaDefinitivaUseCase enviarNormativaDefinitivaUseCase(
            ProcesoAcreditacionRepositoryPort procesoRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            com.undec.acreditacion.domain.ports.SolicitudNormativaRepositoryPort solicitudRepository,
            com.undec.acreditacion.domain.ports.DocumentoNormativaRepositoryPort documentoRepository,
            CarreraRepositoryPort carreraRepository,
            RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository,
            com.undec.acreditacion.domain.ports.EmailNotificationPort emailNotificationPort) {
        return new com.undec.acreditacion.application.usecase.EnviarNormativaDefinitivaService(
                procesoRepository, planTrabajoRepository, solicitudRepository, documentoRepository,
                carreraRepository, registroConvocatoriaRepository, emailNotificationPort);
    }

    @Bean
    @Transactional
    public com.undec.acreditacion.application.input.RevisarNormativaUseCase revisarNormativaUseCase(
            ProcesoAcreditacionRepositoryPort procesoRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            com.undec.acreditacion.domain.ports.SolicitudNormativaRepositoryPort solicitudRepository,
            com.undec.acreditacion.domain.ports.DocumentoNormativaRepositoryPort documentoRepository,
            CarreraRepositoryPort carreraRepository,
            RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository,
            com.undec.acreditacion.domain.ports.EmailNotificationPort emailNotificationPort) {
        return new com.undec.acreditacion.application.usecase.RevisarNormativaService(
                procesoRepository, planTrabajoRepository, solicitudRepository, documentoRepository,
                carreraRepository, registroConvocatoriaRepository, emailNotificationPort);
    }

    @Bean
    public com.undec.acreditacion.application.input.ExportarHojaInfoExcelUseCase exportarHojaInfoExcelUseCase(
            PlanTrabajoRepositoryPort planTrabajoRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            CarreraRepositoryPort carreraRepository,
            RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository,
            com.undec.acreditacion.domain.ports.SolicitudNormativaRepositoryPort solicitudRepository,
            com.undec.acreditacion.domain.ports.ExcelReportGeneratorPort excelReportGeneratorPort) {
        return new com.undec.acreditacion.application.usecase.ExportarHojaInfoExcelService(
                planTrabajoRepository, procesoRepository, carreraRepository, registroConvocatoriaRepository,
                solicitudRepository, excelReportGeneratorPort);
    }

    @Bean
    @Transactional
    public com.undec.acreditacion.application.input.IniciarConvocatoriaEscuelaUseCase iniciarConvocatoriaEscuelaUseCase(
            com.undec.acreditacion.domain.ports.ConvocatoriaEscuelaRepositoryPort convocatoriaRepository,
            com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort unidadAcademicaRepository,
            CarreraRepositoryPort carreraRepository) {
        return new com.undec.acreditacion.application.usecase.IniciarConvocatoriaEscuelaService(
                convocatoriaRepository, unidadAcademicaRepository, carreraRepository);
    }

    @Bean
    @Transactional
    public com.undec.acreditacion.application.input.ConfirmarConvocatoriaDirectorEscuelaUseCase confirmarConvocatoriaDirectorEscuelaUseCase(
            com.undec.acreditacion.domain.ports.ConvocatoriaEscuelaRepositoryPort convocatoriaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            RegistroConvocatoriaRepositoryPort registroConvocatoriaRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository,
            CarreraRepositoryPort carreraRepository) {
        return new com.undec.acreditacion.application.usecase.ConfirmarConvocatoriaDirectorEscuelaService(
                convocatoriaRepository, procesoRepository, registroConvocatoriaRepository,
                planTrabajoRepository, carreraRepository);
    }

    @Bean
    public com.undec.acreditacion.application.input.ConsultarConvocatoriaEscuelaUseCase consultarConvocatoriaEscuelaUseCase(
            com.undec.acreditacion.domain.ports.ConvocatoriaEscuelaRepositoryPort convocatoriaRepository,
            com.undec.acreditacion.domain.ports.UnidadAcademicaRepositoryPort unidadAcademicaRepository,
            CarreraRepositoryPort carreraRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository) {
        return new com.undec.acreditacion.application.usecase.ConsultarConvocatoriaEscuelaService(
                convocatoriaRepository, unidadAcademicaRepository, carreraRepository, procesoRepository);
    }
}
