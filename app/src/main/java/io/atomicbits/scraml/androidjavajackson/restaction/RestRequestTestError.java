package io.atomicbits.scraml.androidjavajackson.restaction;

import io.atomicbits.raml10.RamlTestClient;
import io.atomicbits.raml10.User;
import io.atomicbits.raml10.dsl.androidjavajackson.Callback;

/**
 * Created by peter on 8/12/17.
 */

public class RestRequestTestError extends RestAction {

    public RestRequestTestError(RamlTestClient client) {
        super(client);
    }

    @Override
    public void call(ActionFinished finishCallback) {
        setSuccessful(false);
        finishCallback.finished();
    }

    @Override
    public String getName() {
        return "Request test error";
    }

}
