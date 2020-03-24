package org.dev.app.webflux.service.impl;

import org.dev.app.webflux.dao.CategoriaDAO;
import org.dev.app.webflux.dao.ProductoDAO;
import org.dev.app.webflux.documents.Categoria;
import org.dev.app.webflux.documents.Producto;
import org.dev.app.webflux.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service("productoService")
public class ProductoServiceImpl implements ProductoService {

	@Autowired
	@Qualifier("productoRepository")
	private ProductoDAO productoDAO;
	
	@Autowired
	@Qualifier("categoriaRepository")
	private CategoriaDAO categoriaRepository;
	
	@Override
	public Flux<Producto> findAll() {
		return productoDAO.findAll();
	}
	
	@Override
	public Flux<Producto> findAllNombresToUpperCase() {
		Flux<Producto> productos = productoDAO
									.findAll()
									.map((p) -> {
										p.setNombre(p.getNombre().toUpperCase());
										return p;
										});
		return productos;
	}
	
	@Override
	public Flux<Producto> findAllNombresToUpperCaseRepeat() {
		Flux<Producto> productos = findAllNombresToUpperCase()
												.repeat(5000);
		return productos;
	}

	@Override
	public Mono<Producto> findById(String id) {
		return productoDAO.findById(id);
	}

	@Override
	public Mono<Producto> saveProducto(Producto producto) {
		return productoDAO.save(producto);
	}

	@Override
	public Mono<Void> delete(Producto producto) {
		return productoDAO.delete(producto);
	}

	@Override
	public Flux<Categoria> findAllCategorias() {
		return categoriaRepository.findAll();
	}

	@Override
	public Mono<Categoria> findCategoriaById(String id) {
		return categoriaRepository.findById(id);
	}

	@Override
	public Mono<Categoria> saveCategoria(Categoria categoria) {
		return categoriaRepository.save(categoria);
	}
}
