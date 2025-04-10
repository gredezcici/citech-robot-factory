package service;

import entity.Order;
import exception.UnProcessableCompException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Service;
import repository.InventoryRepository;
import repository.OrderRepository;
import util.Calculator;

import java.util.*;

/**
 * @author chaochen
 */
@Service
@EnableAutoConfiguration
public class OrderProcessingService {

    private final InventoryRepository inventoryRepository;

    private final OrderRepository orderRepository;

    @Autowired
    public OrderProcessingService(InventoryRepository inventoryRepo, OrderRepository orderRepo) {
        inventoryRepository = inventoryRepo;
        orderRepository = orderRepo;
    }

    public Order serarchOrder(String orderId) {
        return orderRepository.searchOrder(orderId);
    }


    public Order createOrder(List<String> components) throws UnProcessableCompException {

        Map<String, Integer> parts = new HashMap<>();
        for (String code : components) {
            parts.put(code, 1);
        }
        placeOrder(parts);
        double total = calTotalPrice(components);
        List<String> desc = getPartsList(components);
        Order order = new Order("new", total, desc);
        orderRepository.upsertOrder(order);
        return order;
    }

    private double calTotalPrice(List<String> codes) {
        double sum = 0;
        for (String code : codes) {
            sum = Calculator.add(sum, inventoryRepository.getPriceByCode(code));
        }
        return sum;
    }

    private List<String> getPartsList(List<String> codes) {
        List<String> partsDesc = new ArrayList<>();
        for (String code : codes) {
            String desc = inventoryRepository.getDescByCode(code);
            if (Optional.ofNullable(desc).isPresent()) {
                partsDesc.add(code + ":" + desc);
            }
        }
        return partsDesc;
    }

    private boolean validateCompTypes(List<String> codes) {
        Set<String> types = new HashSet<>();
        for (String code : codes) {
            String type = inventoryRepository.getTypeByCode(code);
            if (Optional.ofNullable(type).isEmpty() || !types.add(type)) return false;
        }
        return types.size() == 4;
    }

    private void placeOrder(Map<String, Integer> codes) throws UnProcessableCompException {
        inventoryRepository.reduceInventoryAvail(codes);
    }
    public int[] leftmostBuildingQueries(int[] heights, int[][] queries) {
        int n = heights.length, qn = queries.length;
        List<int[]>[] que = new ArrayList[n];
        for (int i = 0; i < n; i++)
            que[i] = new ArrayList();
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        int[] res = new int[qn];
        Arrays.fill(res, -1);
        // Step 1
        for (int qi = 0; qi < qn; qi++) {
            int i = queries[qi][0], j = queries[qi][1];
            if (i < j && heights[i] < heights[j]) {
                res[qi] = j;
            } else if (i > j && heights[i] > heights[j]) {
                res[qi] = i;
            } else if (i == j) {
                res[qi] = i;
            } else { // Step 2
                que[Math.max(i, j)].add(new int[]{Math.max(heights[i], heights[j]), qi});
            }
        }
        // Step 3
        for (int i = 0; i < n; i++) {
            while (!pq.isEmpty() && pq.peek()[0] < heights[i]) {
                res[pq.poll()[1]] = i;
            }
            for (int[] q : que[i]) {
                pq.add(q);
            }
        }

        return res;
    }
}
