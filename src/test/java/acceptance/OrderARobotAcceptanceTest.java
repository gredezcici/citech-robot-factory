package acceptance;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.citech.citechrobotfactory.CitechRobotFactoryApplication;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

/** @author chaochen */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {CitechRobotFactoryApplication.class})
public class OrderARobotAcceptanceTest {
  @LocalServerPort private int springBootPort;

  public int getSpringBootPort() {
    return this.springBootPort;
  }

  /* should order a robot*/
  @Test
  void should_order_a_robot() {
    this.postOrder(
            "{ \n"
                + "                        \"components\": [\"I\",\"A\",\"D\",\"F\"]\n"
                + "                    }")
        .then()
        .assertThat()
        .statusCode(HttpStatus.CREATED.value())
        .body("order_id", CoreMatchers.notNullValue(), new Object[0])
        .body("total", CoreMatchers.equalTo(160.11F), new Object[0]);
  }

  @Test
  void should_handle_precision_of_order_price() {
    this.postOrder(
            "{ \n"
                + "                        \"components\": [\"I\",\"A\",\"E\",\"G\"]\n"
                + "                    }")
        .then()
        .assertThat()
        .statusCode(HttpStatus.CREATED.value())
        .body("order_id", CoreMatchers.notNullValue(), new Object[0])
        .body("total", CoreMatchers.equalTo(167.92F), new Object[0]);
  }

  @Test
  void should_not_allow_out_of_stock_order() {
    this.postOrder(
            "{ \n"
                + "                        \"components\": [\"I\",\"C\",\"D\",\"F\"]\n"
                + "                    }")
        .then()
        .assertThat()
        .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
  }

  /* should not allow invalid body*/
  @Test
  void should_not_allow_invalid_body() {
    this.postOrder(
            "{ \n"
                + "                        \"components\": \"BENDER\"\n"
                + "                    }")
        .then()
        .assertThat()
        .statusCode(HttpStatus.BAD_REQUEST.value());
  }

  /* should not allow invalid robot configuration*/
  @Test
  void should_not_allow_invalid_robot_configuration() {
    this.postOrder(
            "{\n"
                + "                        \"components\": [\"A\", \"C\", \"I\", \"D\"]\n"
                + "                    }")
        .then()
        .assertThat()
        .statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
  }

  @Test
  void should_not_allow_GET_method_on_orders() {
    this.getReq("7da588a6-c766-46ce-ad62-431d08ed6995")
        .then()
        .assertThat()
        .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value());
  }

  @Test
  void should_not_allow_wrong_media_type_on_orders() {
    this.postOrderWithText(
            "{ \n"
                + "                        \"components\": [\"I\",\"A\",\"D\",\"F\"]\n"
                + "                    }")
        .then()
        .assertThat()
        .statusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
  }

  private Response getReq(String orderid) {
    return RestAssured.given()
        .contentType(ContentType.JSON)
        .param("id", orderid)
        .when()
        .port(this.getSpringBootPort())
        .get("/orders")
        .then()
        .extract()
        .response();
  }

  private Response postOrder(String body) {
    return RestAssured.given()
        .body(body)
        .contentType(ContentType.JSON)
        .when()
        .port(this.getSpringBootPort())
        .post("/orders", new Object[0]);
  }

  private Response postOrderWithText(String body) {
    return RestAssured.given()
        .body(body)
        .contentType(ContentType.TEXT)
        .when()
        .port(this.getSpringBootPort())
        .post("/orders", new Object[0]);
  }
}
