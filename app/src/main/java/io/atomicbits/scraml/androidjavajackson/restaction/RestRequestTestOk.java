package io.atomicbits.scraml.androidjavajackson.restaction;

import io.atomicbits.raml10.User;
import io.atomicbits.raml10.dsl.androidjavajackson.Callback;

/**
 * Created by peter on 8/12/17.
 */

public class RestRequestTestOk extends RestAction {

    @Override
    public <User> void call(Callback<User> callback) {
        callback.onOkResponse(null);
    }

    @Override
    public String getName() {
        return "Request test OK";
    }

}
