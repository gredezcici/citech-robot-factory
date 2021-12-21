package acceptance;


import db.Column;
import db.DBInternalException;
import db.InMemDatabase;
import entity.RobotPart;
import org.citech.citechrobotfactory.CitechRobotFactoryApplication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

/**
 * @author chaochen
 */

@SpringBootTest(classes = {CitechRobotFactoryApplication.class})
public class InMemDBTest {
    private static final String PRICE = "price";
    private static final String CODE = "code";
    private static final String PART = "part";
    private static final String TYPE = "type";
    private static final String AVAILABLE = "available";
    private static final String DB_NAME = "test";
    private static final InMemDatabase DATABASE = new InMemDatabase(DB_NAME);

    @BeforeAll
    static void init_store() {
        RobotPart robotPart = new RobotPart("O", "Material", "Material X", 0.1, 8);
        RobotPart robotFace = new RobotPart("P", "Face", "Invisible Face", 0.1, 7);
        RobotPart robotMobility = new RobotPart("Q", "Mobility", "Mobility with antigravity engine", 0.1, 6);
        RobotPart robotArm = new RobotPart("R", "Arms", "Arms with laser", 0.1, 5);
        insertNewRow(robotPart);
        insertNewRow(robotFace);
        insertNewRow(robotMobility);
        insertNewRow(robotArm);
    }

    static void insertNewRow(RobotPart part) {
        DATABASE.insertNewRow(part.getCode());
        DATABASE.addColumnToRow(part.getCode(), new Column<String>(CODE, part.getCode()));
        DATABASE.addColumnToRow(part.getCode(), new Column<String>(TYPE, part.getType()));
        DATABASE.addColumnToRow(part.getCode(), new Column<String>(PART, part.getPart()));
        DATABASE.addColumnToRow(part.getCode(), new Column<Integer>(AVAILABLE, part.getAvailable()));
        DATABASE.addColumnToRow(part.getCode(), new Column<Double>(PRICE, part.getPrice()));
    }

    @Test
    void should_get_columns() {
        String rowKey = "Q";
        List<String> columns = Arrays.asList(TYPE, PART, AVAILABLE);
        List<Column> columnList = DATABASE.getMultiColumnsFromRow(rowKey, columns);
        List<String> names = columnList.stream().map(col -> col.getName()).collect(Collectors.toList());
        assertThat(names).containsOnly(PART, AVAILABLE, TYPE);
        List<String> allCol = DATABASE.getAllColumnNamesInRow(rowKey);
        assertThat(allCol).containsOnly(PRICE, PART, AVAILABLE, TYPE, CODE);
        List<String> allCol2 = DATABASE.getAllColumnNamesInRow("xz");
        assertThat(allCol2).isEmpty();
        List<String> allCol3 = DATABASE.getAllColumnNamesInRow("yz");
        assertThat(allCol3).isEmpty();
    }

    @Test
    void should_not_allow_add_column_with_duplicate_name() {
        Column<String> col = new Column<>(PART, "X face");
        assertThatThrownBy(() -> DATABASE.addColumnToRow("P", col))
                .isInstanceOf(DBInternalException.class).hasMessageContaining("duplicate column name found");
    }

}
