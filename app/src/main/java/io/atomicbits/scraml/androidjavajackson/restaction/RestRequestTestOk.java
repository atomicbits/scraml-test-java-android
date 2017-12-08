package io.atomicbits.scraml.androidjavajackson.restaction;

import java.util.Arrays;

import io.atomicbits.raml10.RamlTestClient;
import io.atomicbits.raml10.User;
import io.atomicbits.raml10.dsl.androidjavajackson.Callback;
import io.atomicbits.raml10.dsl.androidjavajackson.Response;

/**
 * Created by peter on 8/12/17.
 */

public class RestRequestTestOk extends RestAction {

    public RestRequestTestOk(RamlTestClient client) {
        super(client);
    }

    @Override
    public void call(final ActionFinished finishCallback) {

        System.out.println("Calling REST service...");

        getClient()
                .rest.user.get(51L, "John J.", null, Arrays.asList("ESA", "NASA"))
                .call(new Callback<User>() {
                    @Override
                    public void onFailure(Throwable t) {
                        setErrorMessage(t.getMessage());
                        setSuccessful(false);
                        finishCallback.finished();
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        setErrorMessage(response.getStringBody());
                        setSuccessful(false);
                        finishCallback.finished();
                    }

                    @Override
                    public void onOkResponse(Response<User> response) {

                        setSuccessful(true);
                        finishCallback.finished();
                    }
                });

    }

    @Override
    public String getName() {
        return "Request test OK";
    }

}
