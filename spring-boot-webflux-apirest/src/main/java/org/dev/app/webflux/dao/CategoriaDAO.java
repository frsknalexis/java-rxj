package org.dev.app.webflux.dao;

import org.dev.app.webflux.documents.Categoria;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

@Repository("categoriaRepository")
public interface CategoriaDAO extends ReactiveMongoRepository<Categoria, String> {

	Mono<Categoria> findByNombre(String nombre);
	
}
