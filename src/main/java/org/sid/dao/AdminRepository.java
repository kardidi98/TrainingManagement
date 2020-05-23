package org.sid.dao;

import java.util.List;

import org.sid.entities.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminRepository extends JpaRepository<Admin, Long> {
	@Query("select a from Admin a where email like :x")
	public Admin findByEmail(@Param("x") String email);
	
	@Query(value="select count(*) from client",nativeQuery = true)
	public long countUsers();
}
