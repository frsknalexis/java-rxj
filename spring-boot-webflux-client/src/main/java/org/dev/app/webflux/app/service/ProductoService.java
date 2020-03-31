package org.dev.app.webflux.app.service;

import org.dev.app.webflux.app.model.Producto;
import org.springframework.http.codec.multipart.FilePart;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductoService {

	Flux<Producto> findAll();
	
	Mono<Producto> findById(String id);
	
	Mono<Producto> saveProducto(Producto producto);
	
	Mono<Producto> updateProducto(Producto producto, String id);
	
	Mono<Void> deleteProducto(String id);
	
	Mono<Producto> uploadFile(FilePart file, String id);
}
