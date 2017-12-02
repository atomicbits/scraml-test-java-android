package io.atomicbits.raml10;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.atomicbits.raml10.dsl.androidjavajackson.BinaryData;
import io.atomicbits.raml10.dsl.androidjavajackson.BodyPart;
import io.atomicbits.raml10.dsl.androidjavajackson.Callback;
import io.atomicbits.raml10.dsl.androidjavajackson.Response;
import io.atomicbits.raml10.dsl.androidjavajackson.StringPart;
import io.atomicbits.raml10.dsl.androidjavajackson.client.ClientConfig;
import io.atomicbits.raml10.rest.user.UserResource;
import io.atomicbits.raml10.rest.user.formurlencodedtype.FormurlencodedtypeResource;
import io.atomicbits.raml10.rest.user.userid.UseridResource;

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


    @Test
    public void getRequestTestError() {

        UserResource userResource = client.rest.user;
        String errorMessage = "Oops";

        // '[]' url-encoded gives: %5B%5D
        stubFor(get(urlEqualTo("/rest/user?firstName=John&organization%5B%5D=ESA&organization%5B%5D=NASA&age=51"))
                .withHeader("Accept", equalTo("application/vnd-v1.0+json"))
                .willReturn(aResponse()
                        .withBody(errorMessage)
                        .withStatus(500)));

        JsonNodeFactory nodeFactory = new JsonNodeFactory(false);
        ObjectNode node = nodeFactory.objectNode();
        node.put("text", "foobar");

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualResponse =
                new AtomicReference<Response<String>>();

        userResource
                .get(51L, "John", null, Arrays.asList("ESA", "NASA"))
                .call(new Callback<User>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        eventualResponse.set(response);
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<User> response) {
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<String> userResponse = eventualResponse.get();
        assertEquals(500, userResponse.getStatus());
        assertEquals(errorMessage, userResponse.getStringBody());

    }


    @Test
    public void postRequestTest() {

        UseridResource userFoobarResource = client.rest.user.userid("foobar");

        stubFor(
                post(urlEqualTo("/rest/user/foobar"))
                        // okhttp3 strips the "; charset=UTF-8" part from a urlencoded form for some reason...
                        // .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded; charset=UTF-8"))
                        .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                        .withHeader("Accept", equalTo("application/json")) // The default media type applies here!
                        .withRequestBody(equalTo("text=Hello-Foobar")) // "text=Hello%20Foobar"
                        .willReturn(
                                aResponse()
                                        .withBody("Post OK")
                                        .withStatus(200)
                        )
        );

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualPostResponse =
                new AtomicReference<Response<String>>();


        userFoobarResource
                .post("Hello-Foobar", null)
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        eventualPostResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        String responseText = eventualPostResponse.get().getBody();
        assertEquals("Post OK", responseText);

    }


    @Test
    public void postWithTypedFormUrlEncodedTest() {

        FormurlencodedtypeResource formurlencodedtypeResource = client.rest.user.formurlencodedtype;

        stubFor(
                post(urlEqualTo("/rest/user/formurlencodedtype"))
                        // okhttp3 strips the "; charset=UTF-8" part from a urlencoded form for some reason...
                        // .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded; charset=UTF-8"))
                        .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
                        .withHeader("Accept", equalTo("application/json")) // The default media type applies here!
                        .withRequestBody(equalTo("firstname=Foo&age=21&lastname=Bar"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                        )
        );

        SimpleForm simpleForm = new SimpleForm();
        simpleForm.setAge(21L);
        simpleForm.setFirstname("Foo");
        simpleForm.setLastname("Bar");

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualPostResponse =
                new AtomicReference<Response<String>>();


        formurlencodedtypeResource
                .post(simpleForm)
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        eventualPostResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<String> response = eventualPostResponse.get();
        assertEquals(200, response.getStatus());

    }


    @Test
    public void getWithTypedQueryStringTest() {

        stubFor(
                get(urlEqualTo("/rest/user/typedquerystring?firstname=Foo&lastname=Bar"))
                        .willReturn(aResponse()
                                .withStatus(200)));

        SimpleForm simpleForm = new SimpleForm();
        // simpleForm.setAge(21L);
        simpleForm.setFirstname("Foo");
        simpleForm.setLastname("Bar");

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualGetResponse =
                new AtomicReference<Response<String>>();


        client.rest.user.typedquerystring
                .get(simpleForm)
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        eventualGetResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<String> response = eventualGetResponse.get();
        assertEquals(200, response.getStatus());

    }


    @Test
    public void putRequestTest() {

        User user = new User(
                new UserDefinitionsAddress("LA", "California", "Mulholland Drive"),
                21L,
                "Doe",
                new Link(null, "http://foo.bar", Method.space),
                "1",
                "John",
                null);

        Link link = new Link(null, "http://foo.bar", Method.$8Trees);

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            stubFor(
                    put(urlEqualTo("/rest/user/foobar"))
                            .withHeader("Content-Type", equalTo("application/vnd-v1.0+json; charset=UTF-8"))
                            .withHeader("Accept", equalTo("application/vnd-v1.0+json"))
                            .withRequestBody(equalTo(objectMapper.writeValueAsString(user)))
                            .willReturn(
                                    aResponse()
                                            .withBody(objectMapper.writeValueAsString(link))
                                            .withStatus(200)
                            )
            );
        } catch (JsonProcessingException e) {
            fail("Did not expect exception: " + e.getMessage());
        }


        UseridResource userFoobarResource = client.rest.user.userid("foobar");

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<Link>> eventualPutResponse =
                new AtomicReference<Response<Link>>();


        userFoobarResource.contentApplicationVndV10Json
                .put(user)
                .call(new Callback<Link>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<Link> response) {
                        eventualPutResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Link receivedLink = eventualPutResponse.get().getBody();
        assertEquals(link.getAccept(), receivedLink.getAccept());
        assertEquals(link.getHref(), receivedLink.getHref());
        assertEquals(link.getMethod(), receivedLink.getMethod());

    }


    @Test
    public void deleteRequestTest() {

        stubFor(
                delete(urlEqualTo("/rest/user/foobar"))
                        .willReturn(
                                aResponse()
                                        .withBody("Delete OK")
                                        .withStatus(200)
                        )
        );

        UseridResource userFoobarResource = client.rest.user.userid("foobar");

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualDeleteResponse =
                new AtomicReference<Response<String>>();


        userFoobarResource
                .delete()
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        eventualDeleteResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        String deleteResponseText = eventualDeleteResponse.get().getBody();
        assertEquals("Delete OK", deleteResponseText);

    }


    @Test
    public void setHeaderRequestTest() {

        stubFor(
                delete(urlEqualTo("/rest/user/foobar"))
                        .withHeader("Accept", equalTo("foo/bar"))
                        .willReturn(
                                aResponse()
                                        .withBody("Delete OK")
                                        .withStatus(200)
                        )
        );

        UseridResource userFoobarResource = client.rest.setHeader("Accept", "*/*").user.setHeader("Accept", "foo/bar").userid("foobar");

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualDeleteResponse =
                new AtomicReference<Response<String>>();


        userFoobarResource
                .delete()
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        eventualDeleteResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        String deleteResponseText = eventualDeleteResponse.get().getBody();
        assertEquals("Delete OK", deleteResponseText);

    }


    @Test
    public void multipartFormRequestTest() {

        stubFor(
                post(urlEqualTo("/rest/user/upload"))
                        .withHeader("Content-Type", equalTo("multipart/form-data; charset=UTF-8"))
                        .willReturn(
                                aResponse()
                                        .withBody("Post OK")
                                        .withStatus(200)
                        )
        );

        List<BodyPart> bodyParts =
                Collections
                        .singletonList(
                                (BodyPart) new StringPart("test", "string part value")
                        );

        client.rest.user.upload
                .post(bodyParts)
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {

                    }

                    @Override
                    public void onNokResponse(Response<String> response) {

                    }

                    @Override
                    public void onOkResponse(Response<String> response) {

                    }
                });

        // ToDo...
    }


    @Test
    public void listRequestTest() {

        User user = new User(
                new UserDefinitionsAddress("LA", "California", "Mulholland Drive"),
                21L,
                "John",
                new Link(null, "http://foo.bar", Method.GET),
                "1",
                "Doe",
                null);

        ObjectMapper objectMapper = new ObjectMapper();

        List<User> users = Collections.singletonList(user);

        try {
            stubFor(
                    put(urlEqualTo("/rest/user/activate"))
                            .withHeader("Content-Type", equalTo("application/vnd-v1.0+json; charset=UTF-8"))
                            .withHeader("Accept", equalTo("application/vnd-v1.0+json"))
                            .withRequestBody(equalTo(objectMapper.writeValueAsString(users)))
                            .willReturn(
                                    aResponse()
                                            .withBody(objectMapper.writeValueAsString(users))
                                            .withStatus(202)
                            )
            );
        } catch (JsonProcessingException e) {
            fail("Did not expect exception: " + e.getMessage());
        }

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<List<User>>> listBodyResponse =
                new AtomicReference<Response<List<User>>>();

        client.rest
                .user.addHeader("Content-Type", "application/vnd-v1.0+json; charset=UTF-8")
                .activate
                .put(users)
                .call(new Callback<List<User>>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<List<User>> response) {
                        listBodyResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        List<User> receivedUsers = listBodyResponse.get().getBody();
        assertEquals(1, receivedUsers.size());
        assertEquals("John", receivedUsers.get(0).getFirstName());

    }


    @Test
    public void listPostRequestTest() {


        stubFor(
                post(urlEqualTo("/rest/animals"))
                        .withRequestBody(equalTo("[\"1\",\"2\"]"))
                        .willReturn(
                                aResponse()
                                        .withBody("[{\"_type\":\"Dog\",\"canBark\":true,\"gender\":\"female\",\"name\":\"Ziva\"}]")
                                        .withStatus(200)
                        )
        );


        List<String> ids = Arrays.asList("1", "2");

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<List<Animal>>> listBodyResponse =
                new AtomicReference<Response<List<Animal>>>();

        client.rest.animals
                .post(new ArrayList<>(ids))
                .call(new Callback<List<Animal>>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<List<Animal>> response) {
                        listBodyResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        List<Animal> receivedUsers = listBodyResponse.get().getBody();
        assertEquals(1, receivedUsers.size());

    }


    @Test
    public void classListAndGenericsRequestTest() {

        stubFor(
                get(urlEqualTo("/rest/animals"))
                        .willReturn(
                                aResponse()
                                        .withBody("[{\"_type\":\"Dog\",\"canBark\":true,\"gender\":\"female\",\"name\":\"Ziva\"}]")
                                        .withStatus(200)
                        )
        );

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<List<Animal>>> eventualAnimal =
                new AtomicReference<Response<List<Animal>>>();

        client.rest.animals
                .get()
                .call(new Callback<List<Animal>>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<List<Animal>> response) {
                        eventualAnimal.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        List<Animal> animals = eventualAnimal.get().getBody();
        assertEquals(1, animals.size());
        Animal animal = animals.get(0);
        assertTrue(animal instanceof Dog);
        Dog theDog = (Dog) animal;
        assertEquals("Ziva", theDog.getName());
    }


    @Test
    public void putListRequestTest() {

        List<Animal> animals = Arrays.asList(
                new Dog(true, "male", "Wiskey"),
                new Fish("female"),
                new Cat("male", "Duster")
        );

        stubFor(
                put(urlEqualTo("/rest/animals"))
                        .withRequestBody(equalTo("[{\"_type\":\"Dog\",\"gender\":\"male\",\"canBark\":true,\"name\":\"Wiskey\"},{\"_type\":\"Fish\",\"gender\":\"female\"},{\"_type\":\"Cat\",\"gender\":\"male\",\"name\":\"Duster\"}]"))
                        .willReturn(
                                aResponse()
                                        .withBody("[{\"_type\":\"Cat\",\"gender\":\"female\",\"name\":\"Orelia\"}]")
                                        .withStatus(200)
                        )
        );

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<List<Animal>>> eventualAnimals =
                new AtomicReference<Response<List<Animal>>>();

        client.rest.animals
                .put(animals)
                .call(new Callback<List<Animal>>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<List<Animal>> response) {
                        eventualAnimals.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        List<Animal> receivedAnimals = eventualAnimals.get().getBody();
        assertEquals(1, receivedAnimals.size());
        Animal animal = receivedAnimals.get(0);
        assertTrue(animal instanceof Cat);
        Cat orelia = (Cat) animal;
        assertEquals("Orelia", orelia.getName());

    }


    @Test
    public void binaryFileUploadTest() throws URISyntaxException, MalformedURLException, UnsupportedEncodingException {

        stubFor(
                post(urlEqualTo("/rest/animals/datafile/upload"))
                        .withRequestBody(equalTo(new String(binaryData(), "UTF-8")))
                        .willReturn(
                                aResponse()
                                        .withBody("{\"received\":\"OK\"}")
                                        .withStatus(200)
                        )
        );

        File file = new File(new URI(this.getClass().getResource("/io/atomicbits/scraml/binaryData.bin").toString()));

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualResponse =
                new AtomicReference<Response<String>>();

        client.rest.animals.datafile.upload
                .post(file)
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        eventualResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<String> response = eventualResponse.get();
        assertEquals(200, response.getStatus());

    }


    @Test
    public void binaryStreamUploadTest() throws URISyntaxException, IOException {

        stubFor(
                post(urlEqualTo("/rest/animals/datafile/upload"))
                        .withRequestBody(equalTo(new String(binaryData(), "UTF-8")))
                        .willReturn(
                                aResponse()
                                        .withBody("{\"received\":\"OK\"}")
                                        .withStatus(200)
                        )
        );

        InputStream inputStream = this.getClass().getResourceAsStream("/io/atomicbits/scraml/binaryData.bin");

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualResponse =
                new AtomicReference<Response<String>>();

        client.rest.animals.datafile.upload
                .post(inputStream)
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        eventualResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<String> response = eventualResponse.get();
        assertEquals(200, response.getStatus());
    }


    @Test
    public void binaryByteArrayUploadTest() throws URISyntaxException, IOException {

        stubFor(
                post(urlEqualTo("/rest/animals/datafile/upload"))
                        .withRequestBody(equalTo(new String(binaryData(), "UTF-8")))
                        .willReturn(
                                aResponse()
                                        .withBody("{\"received\":\"OK\"}")
                                        .withStatus(200)
                        )
        );

        InputStream inputStream = this.getClass().getResourceAsStream("/io/atomicbits/scraml/binaryData.bin");
        byte[] data = new byte[1024];
        inputStream.read(data, 0, 1024);
        inputStream.close();

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualResponse =
                new AtomicReference<Response<String>>();

        client.rest.animals.datafile.upload
                .post(data)
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        eventualResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<String> response = eventualResponse.get();
        assertEquals(200, response.getStatus());

    }


    @Test
    public void binaryStringUploadTest() throws URISyntaxException, IOException {

        String text = "some test string";

        stubFor(
                post(urlEqualTo("/rest/animals/datafile/upload"))
                        .withRequestBody(equalTo(text))
                        .willReturn(
                                aResponse()
                                        .withBody("{\"received\":\"OK\"}")
                                        .withStatus(200)
                        )
        );

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualResponse =
                new AtomicReference<Response<String>>();

        client.rest.animals.datafile.upload
                .post(text)
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        eventualResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<String> response = eventualResponse.get();
        assertEquals(200, response.getStatus());
    }


    @Test
    public void binaryDownloadTest() {
        stubFor(
                get(urlEqualTo("/rest/animals/datafile/download"))
                        .willReturn(
                                aResponse()
                                        .withBody(binaryData())
                                        .withStatus(200)
                        )
        );

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<BinaryData>> eventualResponse =
                new AtomicReference<Response<BinaryData>>();

        client.rest.animals.datafile.download
                .get()
                .call(new Callback<BinaryData>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<BinaryData> response) {
                        eventualResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<BinaryData> response = eventualResponse.get();
        assertEquals(200, response.getStatus());
        try {
            assertArrayEquals(binaryData(), response.getBody().asBytes());
        } catch (IOException e) {
            fail("Did not expect exception: " + e.getMessage());
        }

    }


    // - - - Tests using RAML 1.0 features start here


    /**
     * test a GET request to get a Book list (the base class of a hierarchy)
     */
    @Test
    public void raml10GetBookList() {

        Author jamesCorey = new Author("James", "Corey");
        Author peterDavid = new Author("Peter", "David");

        List<Book> expectedBooks = Arrays.asList(
                new BookImpl(jamesCorey, "SciFi", "978-0-316-12908-4", "Leviathan Wakes"),
                new ComicBook(peterDavid, "SciFi", "Spiderman", "75960608623800111", "The Clone Conspiracy", "Mr. Badguy"),
                new SciFiComicBook(peterDavid, "1990", "SciFi", "Spiderman", "75960608623800111", "The Clone Conspiracy", "Mr. Badguy")
        );

        stubFor(
                get(urlEqualTo("/books"))
                        .withHeader("Accept", equalTo("application/json"))
                        .willReturn(
                                aResponse()
                                        .withBody("[{\"author\": {\"firstName\": \"James\", \"lastName\": \"Corey\"}, \"isbn\":\"978-0-316-12908-4\", \"title\": \"Leviathan Wakes\", \"genre\": \"SciFi\", \"kind\": \"Book\"}, {\"author\": {\"firstName\": \"Peter\", \"lastName\": \"David\"}, \"isbn\":\"75960608623800111\", \"title\": \"The Clone Conspiracy\", \"genre\": \"SciFi\", \"hero\": \"Spiderman\", \"villain\": \"Mr. Badguy\", \"kind\": \"ComicBook\"}, {\"author\": {\"firstName\": \"Peter\", \"lastName\": \"David\"}, \"isbn\":\"75960608623800111\", \"title\": \"The Clone Conspiracy\", \"genre\": \"SciFi\", \"hero\": \"Spiderman\", \"era\": \"1990\", \"villain\": \"Mr. Badguy\", \"kind\": \"ScienceFictionComicBook\"}]")
                                        .withStatus(200)
                        )
        );


        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<List<Book>>> eventualBooks =
                new AtomicReference<Response<List<Book>>>();

        client.books
                .get()
                .call(new Callback<List<Book>>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<List<Book>> response) {
                        eventualBooks.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<List<Book>> response = eventualBooks.get();
        assertEquals(200, response.getStatus());
        List<Book> books = response.getBody();
        assertEquals(3, books.size()); // Todo: full comparison of elements once we have the equals() method in our POJOs!!!
    }


    /**
     * test a POST request with a Book (the base class of a hierarchy)
     */
    @Test
    public void raml10PostBook() {

        stubFor(
                post(urlEqualTo("/books"))
                        .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                        .withRequestBody(
                                equalToJson(
                                        "{\"author\": {\"firstName\": \"James\", \"lastName\": \"Corey\"}, \"isbn\":\"978-0-316-12908-4\", \"title\": \"Leviathan Wakes\", \"genre\": \"SciFi\", \"kind\": \"Book\"}")
                        )
                        .willReturn(aResponse()
                                .withStatus(201)));

        Author jamesCorey = new Author("James", "Corey");
        BookImpl book = new BookImpl(jamesCorey, "SciFi", "978-0-316-12908-4", "Leviathan Wakes");

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualResponse =
                new AtomicReference<Response<String>>();

        client.books
                .post(book)
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        eventualResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<String> response = eventualResponse.get();
        assertEquals(201, response.getStatus());
    }


    /**
     * test a GET request to get a ComicBook list (a non-leaf subclass in a class hierarchy)
     */
    @Test
    public void raml10GetComicBook() {

        stubFor(
                get(urlEqualTo("/books/comicbooks"))
                        .withHeader("Accept", equalTo("application/json"))
                        .willReturn(aResponse()
                                .withBody("[{\"author\": {\"firstName\": \"Peter\", \"lastName\": \"David\"}, \"isbn\":\"75960608623800111\", \"title\": \"The Clone Conspiracy\", \"genre\": \"SciFi\", \"hero\": \"Spiderman\", \"villain\": \"Mr. Badguy\", \"kind\": \"ComicBook\"}]")
                                .withStatus(200)));

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<List<ComicBook>>> eventualComicBooks =
                new AtomicReference<Response<List<ComicBook>>>();

        client.books.comicbooks
                .get()
                .call(new Callback<List<ComicBook>>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<List<ComicBook>> response) {
                        eventualComicBooks.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<List<ComicBook>> comicBookResponse = eventualComicBooks.get();
        assertEquals(200, comicBookResponse.getStatus());
        assertEquals("Peter", comicBookResponse.getBody().get(0).getAuthor().getFirstName());
    }


    /**
     * test a POST request with a ComicBook (a non-leaf subclass in a class hierarchy)
     */
    @Test
    public void raml10PostComicBook() {

        stubFor(
                post(urlEqualTo("/books/comicbooks"))
                        .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                        .withRequestBody(
                                equalToJson(
                                        "{\"author\": {\"firstName\": \"Peter\", \"lastName\": \"David\"}, \"isbn\":\"75960608623800111\", \"title\": \"The Clone Conspiracy\", \"genre\": \"SciFi\", \"hero\": \"Spiderman\", \"villain\": \"Mr. Badguy\", \"kind\": \"ComicBook\"}"
                                )
                        )
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                        )
        );

        Author peterDavid = new Author("Peter", "David");
        ComicBook comicBook = new ComicBook(peterDavid, "SciFi", "Spiderman", "75960608623800111", "The Clone Conspiracy", "Mr. Badguy");

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualResponse =
                new AtomicReference<Response<String>>();

        client.books.comicbooks
                .post(comicBook)
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        eventualResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<String> response = eventualResponse.get();
        assertEquals(201, response.getStatus());
    }


    /**
     * test a GET request to get a SciFi ComicBook list (a leaf subclass in a class hierarchy)
     */
    @Test
    public void raml10GetSciFiComicBookList() {

        stubFor(
                get(urlEqualTo("/books/comicbooks/scificomicbooks"))
                        .withHeader("Accept", equalTo("application/json"))
                        .willReturn(aResponse()
                                .withBody("[{\"author\": {\"firstName\": \"Peter\", \"lastName\": \"David\"}, \"isbn\":\"75960608623800111\", \"title\": \"The Clone Conspiracy\", \"genre\": \"SciFi\", \"hero\": \"Spiderman\", \"villain\": \"Mr. Badguy\", \"era\": \"1990\", \"kind\": \"ScienceFictionComicBook\"}]")
                                .withStatus(200)));

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<List<SciFiComicBook>>> eventualSciFiComicBooks =
                new AtomicReference<Response<List<SciFiComicBook>>>();

        client.books.comicbooks.scificomicbooks
                .get()
                .call(new Callback<List<SciFiComicBook>>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<List<SciFiComicBook>> response) {
                        eventualSciFiComicBooks.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<List<SciFiComicBook>> sciFiComicBookResponse = eventualSciFiComicBooks.get();
        assertEquals(200, sciFiComicBookResponse.getStatus());
        assertEquals("Peter", sciFiComicBookResponse.getBody().get(0).getAuthor().getFirstName());

    }


    /**
     * test a POST request with a SciFi ComicBook (a leaf subclass in a class hierarchy)
     */
    @Test
    public void raml10PostSciFiComicBook() {

        stubFor(
                post(urlEqualTo("/books/comicbooks/scificomicbooks"))
                        .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                        .withRequestBody(
                                equalToJson(
                                        "{\"author\": {\"firstName\": \"Peter\", \"lastName\": \"David\"}, \"isbn\":\"75960608623800111\", \"title\": \"The Clone Conspiracy\", \"genre\": \"SciFi\", \"hero\": \"Spiderman\", \"villain\": \"Mr. Badguy\", \"era\": \"1990\", \"kind\": \"ScienceFictionComicBook\"}"
                                )
                        )
                        .willReturn(
                                aResponse()
                                        .withStatus(201)
                        )
        );

        Author peterDavid = new Author("Peter", "David");
        SciFiComicBook sciFiComicBook = new SciFiComicBook(peterDavid, "1990", "SciFi", "Spiderman", "75960608623800111", "The Clone Conspiracy", "Mr. Badguy");

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualResponse =
                new AtomicReference<Response<String>>();

        client.books.comicbooks.scificomicbooks
                .post(sciFiComicBook)
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        eventualResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<String> response = eventualResponse.get();
        assertEquals(201, response.getStatus());
    }


    /**
     * deserialization of a given object that contains a field that points to an empty object
     */
    @Test
    public void emptyObjectDeserialization() {
        stubFor(
                get(urlEqualTo("/rest/emptyobject"))
                        .withHeader("Accept", equalTo("application/json"))
                        .willReturn(aResponse()
                                .withBody("{\"message\":\"OK\", \"data\": { \"anything\": 123 } }")
                                .withStatus(200)));

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<EmptyObjectField>> eventualEmptyObjectField =
                new AtomicReference<Response<EmptyObjectField>>();

        client.rest.emptyobject
                .get()
                .call(new Callback<EmptyObjectField>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<EmptyObjectField> response) {
                        eventualEmptyObjectField.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<EmptyObjectField> emptyObjectFieldResponse = eventualEmptyObjectField.get();
        assertEquals(200, emptyObjectFieldResponse.getStatus());
        assertEquals(123, emptyObjectFieldResponse.getBody().getData().findPath("anything").asInt());

    }


    /**
     * serialization of a given object that contains a field that points to an empty object
     */
    @Test
    public void emptyObjectSerialization() {
        stubFor(
                post(urlEqualTo("/rest/emptyobject"))
                        .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                        .withRequestBody(
                                equalToJson(
                                        "{\"message\":\"OK\", \"data\": { \"anything\": 123.0 } }"
                                )
                        )
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                        )
        );

        JsonNodeFactory factory = JsonNodeFactory.instance;
        EmptyObjectField emptyObjectField =
                new EmptyObjectField(factory.objectNode().set("anything", factory.numberNode(123)), "OK");

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<String>> eventualResponse =
                new AtomicReference<Response<String>>();

        client.rest.emptyobject
                .post(emptyObjectField)
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        eventualResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<String> response = eventualResponse.get();
        assertEquals(200, response.getStatus());

    }


    /**
     * A plain string post body should serialize without extra quotes.
     */
    @Test
    public void plainStringPostBody() {
        stubFor(
                post(urlEqualTo("/rest/animals/food"))
                        .withHeader("Content-Type", equalTo("application/json; charset=UTF-8"))
                        .withRequestBody(
                                equalTo("veggie")
                        )
                        .willReturn(
                                aResponse()
                                        .withBody("[{\"_type\":\"Cat\",\"gender\":\"female\",\"name\":\"Orelia\"}]")
                                        .withStatus(200)
                        )
        );

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<List<Animal>>> eventualResponse =
                new AtomicReference<Response<List<Animal>>>();

        client.rest.animals.food
                .post("veggie")
                .call(new Callback<List<Animal>>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<List<Animal>> response) {
                        eventualResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<List<Animal>> response = eventualResponse.get();
        assertEquals(200, response.getStatus());

    }


    /**
     * An enumeration as query parameter type should serialize as its string value
     */
    @Test
    public void enumerationQueryParameterType() {

        stubFor(get(urlEqualTo("/rest/animals/byfood?food=rats"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withBody("[{\"_type\":\"Dog\",\"canBark\":true,\"gender\":\"female\",\"name\":\"Ziva\"}]")
                        .withStatus(200)));

        final AtomicBoolean callbackFinished = new AtomicBoolean(false);
        final AtomicReference<Response<List<Animal>>> eventualResponse =
                new AtomicReference<Response<List<Animal>>>();

        client.rest.animals.byfood
                .get(Food.rats)
                .call(new Callback<List<Animal>>() {
                    @Override
                    public void onFailure(Throwable t) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        callbackFinished.set(true);
                    }

                    @Override
                    public void onOkResponse(Response<List<Animal>> response) {
                        eventualResponse.set(response);
                        callbackFinished.set(true);
                    }
                });

        await().untilTrue(callbackFinished);

        Response<List<Animal>> response = eventualResponse.get();
        assertEquals(200, response.getStatus());

    }


    private byte[] binaryData() {
        byte[] data = new byte[1024];
        for (int i = 0; i < 1024; i++) {
            data[i] = (byte) i;
        }
        return data;
    }

    /**
     * Code to create the binary test file.
     */
    private void createBinaryDataFile() throws FileNotFoundException, IOException {
        File file = new File("binaryData.bin");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(binaryData());
        fileOutputStream.flush();
        fileOutputStream.close();
    }


}
