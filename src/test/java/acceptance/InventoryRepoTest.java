package acceptance;


import entity.RobotPart;
import exception.UnProcessableCompException;
import org.citech.citechrobotfactory.CitechRobotFactoryApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import repository.InventoryRepository;
import repository.OrderRepository;
import service.OrderProcessingService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;


/**
 * @author chaochen
 */

@SpringBootTest(classes = {CitechRobotFactoryApplication.class})
public class InventoryRepoTest {
    @Autowired
    InventoryRepository inventoryRepository;
    @Autowired
    OrderRepository orderRepository;
    @Autowired
    OrderProcessingService orderProcessingService;

    @Test
    void should_load_stock() {
        double expectedPrice = 10.28;
        String expectedType = "Face";
        String expectedDesc = "Humanoid Face";
        //read from database
        assertThat(inventoryRepository.getPriceByCode("A")).isEqualTo(expectedPrice);
        assertThat(inventoryRepository.getTypeFromDB("A")).isEqualTo(expectedType);
        assertThat(inventoryRepository.getPartByCodeFromDB("A")).isEqualTo(expectedDesc);
        //read from cache
        assertThat(inventoryRepository.getPriceByCode("A")).isEqualTo(expectedPrice);
        assertThat(inventoryRepository.getTypeByCode("A")).isEqualTo(expectedType);
        assertThat(inventoryRepository.getDescByCode("A")).isEqualTo(expectedDesc);
    }

    @Test
    void should_rollback_when_out_of_stock() {
        RobotPart robotPart = new RobotPart("X", "Material", "Material X", 0.1, 8);
        RobotPart robotFace = new RobotPart("L", "Face", "Invisible Face", 0.1, 7);
        RobotPart robotMobility = new RobotPart("M", "Mobility", "Mobility with antigravity engine", 0.1, 6);
        RobotPart robotArm = new RobotPart("N", "Arms", "Arms with laser", 0.1, 5);
        inventoryRepository.insertPartIntoDB(robotPart);
        inventoryRepository.insertPartIntoDB(robotFace);
        inventoryRepository.insertPartIntoDB(robotMobility);
        inventoryRepository.insertPartIntoDB(robotArm);
        Map<String, Integer> components = new HashMap<>();
        components.put("X", 1);
        components.put("L", 1);
        components.put("M", 1);
        components.put("N", 1);
        inventoryRepository.reduceSinglePartInventoryAvail("N", 5);
        assertThat(inventoryRepository.getAvailableFromDB("N")).isZero();
        assertThatThrownBy(() -> inventoryRepository.reduceInventoryAvail(components))
                .isInstanceOf(UnProcessableCompException.class).hasMessageContaining("out of stock");
        // test database
        assertThat(inventoryRepository.getAvailableFromDB("X")).isEqualTo(8);
        assertThat(inventoryRepository.getAvailableFromDB("L")).isEqualTo(7);
        assertThat(inventoryRepository.getAvailableFromDB("M")).isEqualTo(6);
    }

    @Test
    void should_update_cache_after_db_updated() {
        RobotPart robotMaterial = new RobotPart("B", "Material", "Material Y", 0.1, 5);
        inventoryRepository.updatePartExceptAvail(robotMaterial);
        assertThat(inventoryRepository.getTypeFromDB("B")).isEqualTo("Material");
        assertThat(inventoryRepository.getTypeByCode("B")).isEqualTo("Material");
    }
}


