package io.atomicbits.raml10;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.atomicbits.raml10.dsl.androidjavajackson.Callback;
import io.atomicbits.raml10.dsl.androidjavajackson.Response;
import io.atomicbits.raml10.dsl.androidjavajackson.client.ClientConfig;
import io.atomicbits.raml10.rest.user.UserResource;

import static org.junit.Assert.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.awaitility.Awaitility.*;


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class RamlModelGeneratorTest {

    private static int port = 8282;
    private static String host = "localhost";
    private static WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port));
    private static RamlTestClient client;


    @BeforeClass
    public static void setUpClass() {
        wireMockServer.start();
        WireMock.configureFor(host, port);
        Map<String, String> defaultHeaders = new HashMap<>();
        // defaultHeaders.put("Accept", "application/vnd-v1.0+json");
        ClientConfig config = new ClientConfig();
        config.setRequestCharset(Charset.forName("UTF-8"));
        client = new RamlTestClient(host, port, "http", null, config, defaultHeaders);
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        wireMockServer.stop();
        client.close();
    }

    @Test
    public void getRequestTestOk() {

        UserResource userResource = client.rest.user;

        // '[]' url-encoded gives: %5B%5D
        stubFor(get(urlEqualTo("/rest/user?firstName=John%20J.&organization%5B%5D=ESA&organization%5B%5D=NASA&age=51"))
                .withHeader("Accept", equalTo("application/vnd-v1.0+json"))
                .willReturn(aResponse()
                        .withBody(
                                "{\"address\": {\"streetAddress\": \"Mulholland Drive\", \"city\": \"LA\", \"state\": \"California\"}, " +
                                        "\"firstName\":\"John\", " +
                                        "\"lastName\": \"Doë\", " +
                                        "\"age\": 21, " +
                                        "\"id\": \"1\"," +
                                        "\"other\": {\"text\": \"foobar\"}" +
                                        "}")
                        .withHeader("Content-Type", "application/json; charset=UTF-8")
                        .withStatus(200)));


        JsonNodeFactory nodeFactory = new JsonNodeFactory(false);
        ObjectNode node = nodeFactory.objectNode();
        node.put("text", "foobar");


        User expectedUser = new User(new UserDefinitionsAddress("LA", "California", "Mulholland Drive"), 21L, "John", null, "1", "Doë", node);


        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<User>> eventualUser = new AtomicReference<Response<User>>();

        userResource
                .get(51L, "John J.", null, Arrays.asList("ESA", "NASA"))
                .call(new Callback<User>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<User> response) {
                        eventualUser.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<User> userResponse = eventualUser.get();
        User user = userResponse.getBody();
        assertEquals(expectedUser.getFirstName(), user.getFirstName());
        assertEquals(expectedUser.getLastName(), user.getLastName());
        assertEquals(expectedUser.getAge(), user.getAge());
        assertEquals(expectedUser.getAddress().getCity(), user.getAddress().getCity());

    }

    


}