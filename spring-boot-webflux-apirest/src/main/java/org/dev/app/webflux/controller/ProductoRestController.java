package org.dev.app.webflux.controller;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;

import org.dev.app.webflux.documents.Producto;
import org.dev.app.webflux.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/productos")
public class ProductoRestController {
	
	@Autowired
	@Qualifier("productoService")
	private ProductoService productoService;
	
	@Value("${config.uploads.path}")
	private String uploadPath;
	
	@PostMapping("/save-v2")
	public Mono<ResponseEntity<Producto>> saveProductoWithFoto(Producto producto, @RequestPart FilePart file) {
		if (producto.getCreateAt() == null) {
			producto.setCreateAt(new Date());
		}
		
		String nombreFoto = UUID.randomUUID().toString().concat("_").concat(file.filename()
				.replace(" ", "")
				.replace(":", "")
				.replace("\\", ""));
		producto.setFoto(nombreFoto);
		
		return file.transferTo(new File(uploadPath.concat(producto.getFoto())))
				.then(productoService.saveProducto(producto))
				.map((p) -> {
					return ResponseEntity
								.created(URI.create("/api/productos/getOne/".concat(p.getId())))
								.contentType(MediaType.APPLICATION_JSON)
								.body(p);
				});
	}
	
	@PostMapping("/upload/{id}")
	public Mono<ResponseEntity<Producto>> uploadFoto(@PathVariable(value = "id") String id,
				@RequestPart FilePart file) {
		Mono<Producto> producto = productoService.findById(id);
		return producto.flatMap((p) -> {
			String nombreFoto = UUID.randomUUID().toString().concat("_").concat(file.filename()
																					.replace(" ", "")
																					.replace(":", "")
																					.replace("\\", ""));
			p.setFoto(nombreFoto);
			return file.transferTo(new File(uploadPath.concat(p.getFoto())))
						.then(productoService.saveProducto(p));
			})
			.map((p) -> {
				return ResponseEntity.ok(p);
			})
			.defaultIfEmpty(ResponseEntity.notFound().build());
	}
	
	@GetMapping("/listar")
	/* PRIMERA FORMA RETORNANDO EL FLUX DIRECTO
	public Flux<Producto> listarProductos() {
		Flux<Producto> productos = productoService.findAll();
		return productos;
	}*/
	public Mono<ResponseEntity<Flux<Producto>>> listarProducto() {
		Flux<Producto> productos = productoService.findAll();
		return Mono.just(ResponseEntity.ok()
							.contentType(MediaType.APPLICATION_JSON)
							.body(productos));
	}
	
	@GetMapping("/getOne/{id}")
	public Mono<ResponseEntity<Producto>> ver(@PathVariable(value = "id") String id) {
		Mono<Producto> producto = productoService.findById(id);
		return producto.map((p) -> {
			return ResponseEntity.ok()
								.contentType(MediaType.APPLICATION_JSON)
								.body(p);
							})
						.defaultIfEmpty(ResponseEntity.notFound()
														.build());
	}
	
	@PostMapping("/save")
	public Mono<ResponseEntity<Map<String, Object>>> saveProducto(@Valid @RequestBody Mono<Producto> monoProducto) {
		
		Map<String, Object> response = new HashMap<String, Object>();
		
		return monoProducto
				.flatMap((producto) -> {
					if (producto.getCreateAt() == null) {
						producto.setCreateAt(new Date());
					}
					return productoService
							.saveProducto(producto);
				})
				.map((p) -> {
					response.put("producto", p);
					response.put("mensaje", "Producto creado con exito");
					response.put("status", HttpStatus.CREATED);
					return ResponseEntity
							.created(URI.create("/api/productos/getOne/".concat(p.getId())))
							.contentType(MediaType.APPLICATION_JSON)
							.body(response);
				})
				.onErrorResume((t) -> {
					return Mono.just(t)
							.cast(WebExchangeBindException.class)
							.flatMap((ex) -> Mono.just(ex.getFieldErrors()))
							.flatMapMany(Flux::fromIterable)
							.map((fieldError) -> {
								return "El campo ".concat(fieldError.getField().concat(" ".concat(fieldError.getDefaultMessage())));
							}).collectList()
							.flatMap((list) -> {
								response.put("errors", list);
								response.put("status", HttpStatus.BAD_REQUEST);
								return Mono.just(ResponseEntity.badRequest()
																.body(response));
							});
				});
		
		/*
		return productoService
				.saveProducto(producto)
				.map((p) -> {
					return ResponseEntity
								.created(URI.create("/api/productos/getOne/".concat(p.getId())))
								.contentType(MediaType.APPLICATION_JSON)
								.body(p);
				});
		*/
	}
	
	@PutMapping("/update/{id}")
	public Mono<ResponseEntity<Producto>> updateProducto(@RequestBody Producto producto, @PathVariable("id") String id) {
		Mono<Producto> productoOld = productoService.findById(id);
		return productoOld
				.flatMap((p) -> {
					p.setNombre(producto.getNombre());
					p.setPrecio(producto.getPrecio());
					p.setCategoria(producto.getCategoria());
					return productoService.saveProducto(p);
				})
				.map((p) -> {
					return ResponseEntity
								.created(URI.create("/api/productos/getOne/".concat(p.getId())))
								.contentType(MediaType.APPLICATION_JSON)
								.body(p);
				})
				.defaultIfEmpty(ResponseEntity.notFound().build());
	}
	
	@DeleteMapping("/delete/{id}")
	public Mono<ResponseEntity<Void>> eliminarProducto(@PathVariable(value = "id") String id) {
		Mono<Producto> producto = productoService.findById(id);
		return producto
					.flatMap((p) -> {
						return productoService.delete(p)
								.then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)));
					})
					.defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
	}
}
