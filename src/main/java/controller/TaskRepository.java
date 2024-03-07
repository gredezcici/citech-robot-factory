package controller;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

/**
 * @author Chao Chen
 */
@Component
public interface TaskRepository extends CrudRepository<Task, Long> {
//    public Task save(Task task) {
//
//        return new Task("1");
//    }
//
//    public List<Task> findAll() {
//        return new ArrayList<>();
//    }
//
//    public Optional<Task> findById(long id) {
//        Task t = new Task("2");
//        return Optional.ofNullable(t);
//    }
//    public void deleteById(long id){
//
//    }
}
