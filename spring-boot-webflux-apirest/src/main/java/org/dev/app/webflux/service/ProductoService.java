package org.dev.app.webflux.service;

import org.dev.app.webflux.documents.Categoria;
import org.dev.app.webflux.documents.Producto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductoService {

	Flux<Producto> findAll();
	
	Flux<Producto> findAllNombresToUpperCase();
	
	Flux<Producto> findAllNombresToUpperCaseRepeat();
	
	Mono<Producto> findById(String id);
	
	Mono<Producto> saveProducto(Producto producto);
	
	Mono<Void> delete(Producto producto);
	
	Flux<Categoria> findAllCategorias();
	
	Mono<Categoria> findCategoriaById(String id);
	
	Mono<Categoria> saveCategoria(Categoria categoria);
}
