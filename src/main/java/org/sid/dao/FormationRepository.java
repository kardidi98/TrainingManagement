package org.sid.dao;

import java.util.List;

import org.sid.entities.Formation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormationRepository extends JpaRepository<Formation, Long> {
	
}
