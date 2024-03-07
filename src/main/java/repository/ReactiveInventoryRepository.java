//package repository;
//
//import entity.RobotPart;
//import org.springframework.data.cassandra.repository.AllowFiltering;
//import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
///** @author chaochen */
//public interface ReactiveInventoryRepository
//    extends ReactiveCassandraRepository<RobotPart, String> {
//
//  Flux<RobotPart> findByKeyFirstName(final String firstName);
//
//  Mono<RobotPart> findOneByKeyFirstName(final String firstName);
//}
//
