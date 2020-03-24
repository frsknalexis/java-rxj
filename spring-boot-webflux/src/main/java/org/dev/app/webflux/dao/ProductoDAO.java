package org.dev.app.webflux.dao;

import org.dev.app.webflux.documents.Producto;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository("productoDAO")
public interface ProductoDAO extends ReactiveMongoRepository<Producto, String>{

}
