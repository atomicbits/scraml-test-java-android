package io.atomicbits.scraml.androidjavajackson;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.atomicbits.raml10.RamlTestClient;
import io.atomicbits.raml10.dsl.androidjavajackson.Callback;
import io.atomicbits.raml10.dsl.androidjavajackson.Response;
import io.atomicbits.raml10.dsl.androidjavajackson.client.ClientConfig;
import io.atomicbits.scraml.androidjavajackson.restaction.ActionFinished;
import io.atomicbits.scraml.androidjavajackson.restaction.RestAction;
import io.atomicbits.scraml.androidjavajackson.restaction.RestRequestTestError;
import io.atomicbits.scraml.androidjavajackson.restaction.RestRequestTestOk;


public class ScramlTestActivity extends AppCompatActivity {

    // Get ListView object from xml
    private ListView listView;

    private List<RestAction> restActions;

    private List<RestAction> toRun;

    private View runButton;

    private MySimpleArrayAdapter adapter;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_run:
                    startAllTests();
                    return true;
                case R.id.navigation_reset:
                    reset();
                    return false;
                case R.id.navigation_about:
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scraml_test);

        int port = 8284;
        String host = "localhost";
        Map<String, String> defaultHeaders = new HashMap<>();
        ClientConfig config = new ClientConfig();
        config.setRequestCharset(Charset.forName("UTF-8"));
        RamlTestClient client = new RamlTestClient(host, port, "http", null, config, defaultHeaders);

        restActions = new ArrayList<>();
        restActions.add(new RestRequestTestOk(client));
        restActions.add(new RestRequestTestError(client));

        adapter = new MySimpleArrayAdapter(this, restActions);

        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);

        runButton = findViewById(R.id.navigation_run);

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

    }

    private void clearStatus() {
        for (RestAction action : restActions) {
            action.reset();
        }
        adapter.notifyDataSetChanged();
    }

    private void startAllTests() {
        // runButton.setEnabled(false);
        clearStatus();
        toRun = new ArrayList<>();
        toRun.addAll(restActions);
        runNext();
    }

    private void runNext() {
        if (!toRun.isEmpty()) {
            final RestAction action = toRun.remove(0);
            action.call(new ActionFinished() {
                @Override
                public void finished() {
                    adapter.notifyDataSetChanged();
                    runNext();
                }
            });
        } else {
            // runButton.setEnabled(true);
        }
    }

    private void reset() {
        this.toRun = new ArrayList<>();
        clearStatus();
        // runButton.setEnabled(true);
    }

}
