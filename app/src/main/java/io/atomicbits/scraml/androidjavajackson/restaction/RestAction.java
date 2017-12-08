package io.atomicbits.scraml.androidjavajackson.restaction;

import io.atomicbits.raml10.RamlTestClient;
import io.atomicbits.raml10.dsl.androidjavajackson.Callback;

/**
 * Created by peter on 8/12/17.
 */

public abstract class RestAction {

    private RamlTestClient client;

    private Boolean succesful = null;

    private String errorMessage = null;

    public RestAction(RamlTestClient client) {
        this.client = client;
    }

    public abstract void call(ActionFinished finishCallback);

    protected RamlTestClient getClient() {
        return this.client;
    }

    public abstract String getName();

    public String getDescription() {
        if (isSuccessful() != null && getErrorMessage() != null && !isSuccessful()) {
            return getName() + " (" + getErrorMessage() + ")";
        } else {
            return getName();
        }

    }

    public void setSuccessful(Boolean successful) {
        this.succesful = successful;
    }

    public Boolean isSuccessful() {
        return this.succesful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void reset() {
        this.succesful = null;
        this.errorMessage = null;
    }

}
