package org.dev.app.webflux.app.handler;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.dev.app.webflux.app.model.Producto;
import org.dev.app.webflux.app.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ProductoHandler {

	@Autowired
	@Qualifier("productoService")
	private ProductoService productoService;
	
	@Autowired
	private Validator validator;
	
	public Mono<ServerResponse> listarProducto(ServerRequest request) {
		Flux<Producto> productos = productoService.findAll();
		return ServerResponse.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(productos, Producto.class);
	}
	
	public Mono<ServerResponse> verProducto(ServerRequest request) {
		String id = request.pathVariable("id");
		
		return productoService
				.findById(id)
				.flatMap((p) -> {
					return ServerResponse.ok()
							.contentType(MediaType.APPLICATION_JSON)
							.body(BodyInserters.fromValue(p));
				})
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> crearProducto(ServerRequest request) {
		Mono<Producto> monoProducto = request.bodyToMono(Producto.class);
		
		//CON VALIDACIONES
		return monoProducto
				.flatMap((p) -> {
					Errors errors = new BeanPropertyBindingResult(p, Producto.class.getName());
					validator.validate(p, errors);
					if (errors.hasErrors()) {
						return Flux.fromIterable(errors.getFieldErrors())
								.map((e) -> {
									return "El campo ".concat(e.getField().concat(" ".concat(e.getDefaultMessage())));
								})
								.collectList()
								.flatMap((listaErrores) -> {
									return ServerResponse.badRequest()
											.contentType(MediaType.APPLICATION_JSON)
											.body(BodyInserters.fromValue(listaErrores));
								});
					} else {
						if (p.getCreateAt() == null) {
							p.setCreateAt(new Date());
						}
						return productoService.saveProducto(p)
								.flatMap((pDB) -> {
									return ServerResponse
											.created(URI.create("/api/v1/cliente/productos/".concat(pDB.getId())))
											.contentType(MediaType.APPLICATION_JSON)
											.body(BodyInserters.fromValue(pDB));
								});
					}
				});
		
		// SIN VALIDACIONES
		/*
		return monoProducto
				.flatMap((p) -> {
					if (p.getCreateAt() == null) {
						p.setCreateAt(new Date());
					}
					return productoService.saveProducto(p);
				})
				.flatMap((p) -> {
					return ServerResponse.created(URI.create("/api/v1/cliente/productos/".concat(p.getId())))
							.contentType(MediaType.APPLICATION_JSON)
							.body(BodyInserters.fromValue(p));
				});
		*/
	}
	
	public Mono<ServerResponse> editarProducto(ServerRequest request) {
		Mono<Producto> productoRequest = request.bodyToMono(Producto.class);
		String id = request.pathVariable("id");
		Mono<Producto> productoDB = productoService.findById(id);

		return productoDB
				.zipWith(productoRequest)
				.map((tupla) -> {
					Producto pDB = tupla.getT1();
					Producto pR = tupla.getT2();
					pDB.setNombre(pR.getNombre());
					pDB.setPrecio(pR.getPrecio());
					pDB.setCategoria(pR.getCategoria());
					return pDB;
				})
				.flatMap((p) -> {
					return productoService.updateProducto(p, id);
				})
				.flatMap((p) -> {
					return ServerResponse.created(URI.create("/api/v1/cliente/productos/".concat(p.getId())))
								.contentType(MediaType.APPLICATION_JSON)
								.body(BodyInserters.fromValue(p));
				})
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> deleteProducto(ServerRequest request) {
		String id = request.pathVariable("id");
		Mono<Producto> productDB = productoService.findById(id);
		return productDB
				.flatMap((p) -> {
					return productoService.deleteProducto(p.getId())
							.then(ServerResponse.noContent().build());
				})
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> upload(ServerRequest request) {
		String id = request.pathVariable("id");
		return request.multipartData()
					.map((multipart) -> {
						return multipart.toSingleValueMap()
								.get("file");
					})
					.cast(FilePart.class)
					.flatMap((file) -> {
						return productoService.uploadFile(file, id);
					})
					.flatMap((p) -> {
						return ServerResponse
								.created(URI.create("/api/v1/cliente/productos/".concat(p.getId())))
								.contentType(MediaType.APPLICATION_JSON)
								.body(BodyInserters.fromValue(p));
					})
					.switchIfEmpty(ServerResponse.notFound().build());
					
	}
	
	public Mono<ServerResponse> errorHandler(Mono<ServerResponse> response) {
		return response
				.onErrorResume((error) -> {
					WebClientResponseException errorResponse = (WebClientResponseException) error;
					if (errorResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
						Map<String, Object> responseBody = new HashMap<String, Object>();
						responseBody.put("error", "No existe el producto: ".concat(errorResponse.getMessage()));
						responseBody.put("timestamp", new Date());
						responseBody.put("status", errorResponse.getStatusCode().value());
						return ServerResponse.status(HttpStatus.NOT_FOUND)
										.body(BodyInserters.fromValue(responseBody));
					}
					return Mono.error(errorResponse);
				});
	}
}
