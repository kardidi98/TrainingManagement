package org.sid.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Ville {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	private String ville;

	public String getVille() {
		return ville;
	}
	

	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	public void setVille(String ville) {
		this.ville = ville;
	}

	public Ville(String ville) {
		this.ville = ville;
	}

	public Ville() {
		super();
		// TODO Auto-generated constructor stub
	}
	
}
