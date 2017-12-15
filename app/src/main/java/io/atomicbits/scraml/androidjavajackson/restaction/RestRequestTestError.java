package io.atomicbits.scraml.androidjavajackson.restaction;

import io.atomicbits.raml10.RamlTestClient;
import io.atomicbits.raml10.dsl.androidjavajackson.Callback;
import io.atomicbits.raml10.dsl.androidjavajackson.Response;

/**
 * Created by peter on 8/12/17.
 */

public class RestRequestTestError extends RestAction {

    public RestRequestTestError(RamlTestClient client) {
        super(client);
    }

    @Override
    public void call(final ActionFinished finishCallback) {
        getClient()
                .doesntexist
                .get()
                .call(new Callback<String>() {
                    @Override
                    public void onFailure(Throwable t) {
                        setErrorMessage(t.getMessage());
                        t.getStackTrace();
                        setSuccessful(false);
                        finishCallback.finished();
                    }

                    @Override
                    public void onNokResponse(Response<String> response) {
                        setErrorMessage(response.getStringBody());
                        System.out.println(response.getStringBody());
                        setSuccessful(false);
                        finishCallback.finished();
                    }

                    @Override
                    public void onOkResponse(Response<String> response) {
                        setSuccessful(true);
                        finishCallback.finished();
                    }
                });
    }

    @Override
    public String getName() {
        return "Request test error";
    }

}
