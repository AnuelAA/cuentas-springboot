package com.cuentas.backend.application.ports.driving;

import com.cuentas.backend.domain.Interest;
import com.cuentas.backend.domain.Liability;
import com.cuentas.backend.domain.LiabilityValue;
import java.time.LocalDate;
import java.util.List;

public interface LiabilityServicePort {
    Liability createLiability(Long userId, Liability liability);
    Liability getLiability(Long userId, Long liabilityId);
    List<Liability> listLiabilities(Long userId);
    Liability updateLiability(Long userId, Long liabilityId, Liability liability);
    void deleteLiability(Long userId, Long liabilityId);
    LiabilityValue upsertLiabilityValue(Long userId, Long liabilityId, LocalDate valuationDate, Double outstandingBalance, LocalDate endDate);
    Interest createInterest(Long userId, Long liabilityId, String type, Double annualRate, LocalDate startDate);
    List<Interest> getInterests(Long userId, Long liabilityId);
}
