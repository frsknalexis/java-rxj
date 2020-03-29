package org.dev.app.webflux.handler;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

import org.dev.app.webflux.documents.Categoria;
import org.dev.app.webflux.documents.Producto;
import org.dev.app.webflux.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ProductoHandler {
	
	@Autowired
	@Qualifier("productoService")
	private ProductoService productoService;
	
	@Value("{config.uploads.path}")
	private String uploadDir;
	
	@Autowired
	private Validator validator;
	
	public Mono<ServerResponse> crearConFoto(ServerRequest request) {
		
		Mono<Producto> producto = request.multipartData()
									.map((multipart) -> {
										FormFieldPart nombre = (FormFieldPart) multipart.toSingleValueMap().get("nombre");
										FormFieldPart precio = (FormFieldPart) multipart.toSingleValueMap().get("precio");
										FormFieldPart categoriaId = (FormFieldPart) multipart.toSingleValueMap().get("categoria.id");
										FormFieldPart categoriaNombre = (FormFieldPart) multipart.toSingleValueMap().get("categoria.nombre");
										
										Categoria categoria = new Categoria(categoriaNombre.value());
										categoria.setId(categoriaId.value());
										return new Producto(nombre.value(), Double.valueOf(precio.value()), categoria);
									});
		
		return request.multipartData()
						.map((multipart) -> {
							return multipart.toSingleValueMap()
									.get("file");
						})
						.cast(FilePart.class)
						.flatMap((file) -> {
							return producto
									.flatMap((p) -> {
										String fileName = UUID.randomUUID().toString().concat("_")
												.concat(file.filename()
															.replace(" ", "")
															.replace(":", "")
															.replace("\\", ""));
										p.setFoto(fileName);
										p.setCreateAt(new Date());
										return file.transferTo(new File(uploadDir.concat(p.getFoto())))
													.then(productoService.saveProducto(p));
									});
						})
						.flatMap((p) -> {
							return ServerResponse
									.created(URI.create("/api/v2/productos/".concat(p.getId())))
									.contentType(MediaType.APPLICATION_JSON)
									.body(BodyInserters.fromValue(p));
						});
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
						return productoService.findById(id)
									.flatMap((p) -> {
										String fileName = UUID.randomUUID().toString().concat("_")
																.concat(file.filename()
																			.replace(" ", "")
																			.replace(":", "")
																			.replace("\\", ""));
										p.setFoto(fileName);
										return file.transferTo(new File(uploadDir.concat(p.getFoto())))
													.then(productoService.saveProducto(p));
									});
					})
					.flatMap((p) -> {
						return ServerResponse
								.created(URI.create("/api/v2/productos/".concat(p.getId())))
								.contentType(MediaType.APPLICATION_JSON)
								.body(BodyInserters.fromValue(p));
					})
					.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> listarProductos(ServerRequest request) {
		Flux<Producto> productoFlux = productoService.findAll();
		return ServerResponse.ok()
						.contentType(MediaType.APPLICATION_JSON)
						.body(productoFlux, Producto.class);
	}
	
	public Mono<ServerResponse> verDetalle(ServerRequest request) {
		String id = request.pathVariable("id");
		return productoService.findById(id)
							.flatMap((p) -> {
								return ServerResponse.ok()
										.contentType(MediaType.APPLICATION_JSON)
										.body(BodyInserters.fromValue(p));
							})
							.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> saveProducto(ServerRequest request) {
		Mono<Producto> monoProducto = request.bodyToMono(Producto.class);
		
		// con validaciones
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
												.body(BodyInserters.fromValue(listaErrores));
									});
					} else {
						if (p.getCreateAt() == null) {
							p.setCreateAt(new Date());
						}
						return productoService.saveProducto(p)
								.flatMap((pDB) -> {
									return ServerResponse.created(URI.create("/api/v2/productos/".concat(pDB.getId())))
											.contentType(MediaType.APPLICATION_JSON)
											.body(BodyInserters.fromValue(pDB));
								});
					}
				});
		// sin validaciones
//		return monoProducto
//				.flatMap((p) -> {
//					if (p.getCreateAt() == null) {
//						p.setCreateAt(new Date());
//					}
//					return productoService.saveProducto(p);
//				})
//				.flatMap((p) -> {
//					return ServerResponse.created(URI.create("/api/v2/productos/".concat(p.getId())))
//							.contentType(MediaType.APPLICATION_JSON)
//							.body(BodyInserters.fromValue(p));
//				});
	}
	
	public Mono<ServerResponse> editarProducto(ServerRequest request) {
		Mono<Producto> monoProducto = request.bodyToMono(Producto.class);
		String id = request.pathVariable("id");
		
		Mono<Producto> productoDB = productoService.findById(id);
		
		return productoDB
				.zipWith(monoProducto, (pDB, pR) -> {
					pDB.setNombre(pR.getNombre());
					pDB.setPrecio(pR.getPrecio());
					pDB.setCategoria(pR.getCategoria());
					return pDB;
				})
				.flatMap((p) -> {
					return ServerResponse.created(URI.create("/api/v2/productos/".concat(p.getId())))
							.contentType(MediaType.APPLICATION_JSON)
							.body(productoService.saveProducto(p), Producto.class);
				})
				.switchIfEmpty(ServerResponse.notFound().build());
	}
	
	public Mono<ServerResponse> eliminarProducto(ServerRequest request) {
		String id = request.pathVariable("id");
		Mono<Producto> producto = productoService.findById(id);
		return producto
				.flatMap((p) -> {
					return productoService.delete(p)
							.then(ServerResponse.noContent().build());
				})
				.switchIfEmpty(ServerResponse.notFound().build());
	}
}
