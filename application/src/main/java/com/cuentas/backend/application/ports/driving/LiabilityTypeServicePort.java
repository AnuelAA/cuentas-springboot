package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.LiabilityType;
import java.util.List;

public interface LiabilityTypeServicePort {
    List<LiabilityType> getAllLiabilityTypes();
}

