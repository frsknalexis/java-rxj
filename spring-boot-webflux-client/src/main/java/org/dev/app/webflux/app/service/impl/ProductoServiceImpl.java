package org.dev.app.webflux.app.service.impl;

import java.util.Collections;

import org.dev.app.webflux.app.model.Producto;
import org.dev.app.webflux.app.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service("productoService")
public class ProductoServiceImpl implements ProductoService {
	
	@Autowired
	private WebClient webClient;

	@Override
	public Flux<Producto> findAll() {
		Flux<Producto> productos = webClient.get()
					.accept(MediaType.APPLICATION_JSON)
					.exchange()
					.flatMapMany((response) -> {
						return response.bodyToFlux(Producto.class);
					});
						
		return productos;
	}

	@Override
	public Mono<Producto> findById(String id) {
		Mono<Producto> producto = webClient.get()
					.uri("/{id}", Collections.singletonMap("id", id))
					.accept(MediaType.APPLICATION_JSON)
					.exchange()
					.flatMap((response) -> {
						Mono<Producto> p = response.bodyToMono(Producto.class);
						return p;
					});
		return producto;
	}

	@Override
	public Mono<Producto> saveProducto(Producto producto) {
		
		Mono<Producto> p = webClient.post()
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(BodyInserters.fromValue(producto))
					.exchange()
					.flatMap((response) -> {
						Mono<Producto> productoResponse = response.bodyToMono(Producto.class);
						return productoResponse;
					});
		
		return p;
	}

	@Override
	public Mono<Producto> updateProducto(Producto producto, String id) {
		Mono<Producto> p = webClient.put()
					.uri("/{id}", Collections.singletonMap("id", id))
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.body(BodyInserters.fromValue(producto))
					.exchange()
					.flatMap((response) -> {
						Mono<Producto> productoResponse = response.bodyToMono(Producto.class);
						return productoResponse;
					});
		return p;
	}

	@Override
	public Mono<Void> deleteProducto(String id) {
		return webClient.delete()
					.uri("/{id}", Collections.singletonMap("id", id))
					.exchange()
					.then();
	}

	@Override
	public Mono<Producto> uploadFile(FilePart file, String id) {
		MultipartBodyBuilder parts = new MultipartBodyBuilder();
		parts.asyncPart("file", file.content(), DataBuffer.class)
				.headers((h) -> {
					h.setContentDispositionFormData("file", file.filename());
				});
		return webClient.post()
				.uri("/upload/{id}", Collections.singletonMap("id", id))
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.accept(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(parts.build()))
				.exchange()
				.flatMap((response) -> {
					Mono<Producto> p = response.bodyToMono(Producto.class);
					return p;
				});
	}
}
