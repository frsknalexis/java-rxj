package org.dev.app.webflux.dao;

import org.dev.app.webflux.documents.Producto;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

@Repository("productoRepository")
public interface ProductoDAO extends ReactiveMongoRepository<Producto, String>{

	Mono<Producto> findByNombre(String nombre);
	
	@Query("{ 'nombre' : ?0 }")
	Mono<Producto> obtenerPorNombre(String nombre);
}
