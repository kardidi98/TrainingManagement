package org.sid.dao;
import java.util.List;


import org.sid.entities.Local;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface  LocalRepository extends JpaRepository<Local, Long>
{
	@Query(value="select * from Local l where owner like :x",nativeQuery = true)
	public List<Local> findByUserId(@Param("x") Long id);
	
	@Query("SELECT DISTINCT l.ville FROM Local l")
	public List<String> getLocalsVilles();

	@Query(value="select * from Local l where ville like :x",nativeQuery = true)
	public List<Local> findByCity(@Param("x") String ville);

	@Query(value="select email from client where id in (select user_id from formation where local like :x)", nativeQuery = true)
	public List<String> findParticipants(@Param("x") Long Id);

	@Transactional
	@Modifying
	@Query(value="update formation set local = NULL where local like :x",nativeQuery = true)
	public void  localDeleted(@Param("x") Long id);
	
	@Query(value="select * from local where disponibilite_from>=CURDATE() ", nativeQuery = true)
	public List<Local> findByTodaysDate();
	
	
	@Query("SELECT DISTINCT l.category FROM Local l")
	public List<String> getLocalsCategories(); 
}
