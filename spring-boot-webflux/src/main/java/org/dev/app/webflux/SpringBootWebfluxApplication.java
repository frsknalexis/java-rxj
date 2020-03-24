package org.dev.app.webflux;

import java.util.Date;

import org.dev.app.webflux.documents.Categoria;
import org.dev.app.webflux.documents.Producto;
import org.dev.app.webflux.service.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import reactor.core.publisher.Flux;

@SpringBootApplication
public class SpringBootWebfluxApplication implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(SpringBootWebfluxApplication.class);
	
	@Autowired
	@Qualifier("productoService")
	private ProductoService productoService;
	
	@Autowired
	private ReactiveMongoTemplate mongoTemplate;
	
	public static void main(String[] args) {
		SpringApplication.run(SpringBootWebfluxApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		
		mongoTemplate.dropCollection("productos")
					.subscribe();
		
		mongoTemplate.dropCollection("categorias")
					.subscribe();
		
		Categoria electronico = new Categoria("Electronico");
		Categoria deporte = new Categoria("Deporte");
		Categoria computacion = new Categoria("Computacion");
		Categoria muebles = new Categoria("Muebles");
		
		Flux.just(electronico, 
					deporte, 
					computacion, 
					muebles)
			.flatMap(categoria -> {
				return productoService.saveCategoria(categoria);
			})
			.doOnNext(c -> logger.info("Categoria creada: ".concat(c.getNombre()).concat(", Id: ".concat(c.getId()))))
			.thenMany(Flux.just(
							new Producto("TV PANASONIC PANTALLA LCD", 456.89, electronico),
							new Producto("SONY CAMARA HD DIGITAL", 177.89, electronico),
							new Producto("APPLE IPOD", 46.89, electronico),
							new Producto("SONY NOTEBOOK", 846.89, computacion),
							new Producto("HP MULTIFUNCIONAL", 200.89, computacion),
							new Producto("Bianchi Bicicleta", 70.98, deporte),
							new Producto("Mica Comoda 5 Cajones", 150.89, muebles))
						.flatMap(producto -> {
								producto.setCreateAt(new Date());
								return productoService.saveProducto(producto);
						}))
			.subscribe(producto -> System.out.println(producto.getId()));;
		
//		Flux.just(
//					new Producto("TV PANASONIC PANTALLA LCD", 456.89, electronico),
//					new Producto("SONY CAMARA HD DIGITAL", 177.89, electronico),
//					new Producto("APPLE IPOD", 46.89, electronico),
//					new Producto("SONY NOTEBOOK", 846.89, computacion),
//					new Producto("HP MULTIFUNCIONAL", 200.89, computacion),
//					new Producto("Bianchi Bicicleta", 70.98, deporte),
//					new Producto("Mica Comoda 5 Cajones", 150.89, muebles))
//		.flatMap(producto -> {
//					producto.setCreateAt(new Date());
//					return productoService.saveProducto(producto);
//			})
//			.subscribe(producto -> System.out.println(producto.getId()));
	}
}
