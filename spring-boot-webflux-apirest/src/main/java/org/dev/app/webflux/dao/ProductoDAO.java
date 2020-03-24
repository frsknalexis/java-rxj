package org.dev.app.webflux.dao;

import org.dev.app.webflux.documents.Producto;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository("productoRepository")
public interface ProductoDAO extends ReactiveMongoRepository<Producto, String>{

}
