package org.dev.app.webflux.app.model;

import java.io.Serializable;

import javax.validation.constraints.NotEmpty;

public class Categoria implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5747723564003828847L;

	@NotEmpty
	private String id;
	
	private String nombre;

	public Categoria() {
		
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
}
