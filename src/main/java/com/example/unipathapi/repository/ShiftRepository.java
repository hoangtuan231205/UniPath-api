package com.example.unipathapi.repository;

import com.example.unipathapi.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Integer> {

    @Query("SELECT s FROM Shift s WHERE s.employment.company.id = :companyId AND s.shiftDate = :shiftDate")
    List<Shift> findShiftsByCompanyAndDate(@Param("companyId") Integer companyId, @Param("shiftDate") LocalDate shiftDate);
}
