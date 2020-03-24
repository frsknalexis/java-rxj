package org.dev.app.webflux.controllers;

import org.dev.app.webflux.dao.ProductoDAO;
import org.dev.app.webflux.documents.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/productos")
public class ProductoRestController {

	@Autowired
	@Qualifier("productoDAO")
	private ProductoDAO productoDAO;
	
	private static final Logger logger = LoggerFactory.getLogger(ProductoRestController.class);
	
	@GetMapping("/listar")
	public Flux<Producto> listar() {
		
		Flux<Producto> productos = productoDAO.findAll()
									.map((producto) -> {
										producto.setNombre(producto.getNombre().toLowerCase());
										return producto;
									})
									.doOnNext((producto) -> logger.info(producto.getNombre()));
		return productos;
	}
	
	@GetMapping("/getOne/{id}")
	Mono<Producto> getOne(@PathVariable(value = "id") String id) {
		
		Flux<Producto> productos = productoDAO.findAll();
		
		Mono<Producto> producto = productos.filter((p) -> {
			return p.getId().equals(id);
		})
		.next()
		.doOnNext((p) -> logger.info(p.getNombre()));
		return producto;
	}
}
