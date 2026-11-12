package com.undec.acreditacion.application.ports;

import java.util.Set;
import java.util.UUID;

public interface SincronizacionAutoevaluacionPort {

    ResultadoSincronizacionDto copiarDatosCentrales(UUID procesoId, ModoClonacion modo, Set<SeccionClonacion> secciones);
}
