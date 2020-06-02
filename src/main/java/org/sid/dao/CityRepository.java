package org.sid.dao;

import org.sid.entities.Ville;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<Ville, Long> {

}
