package io.atomicbits.scraml.androidjavajackson;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import io.atomicbits.raml10.dsl.androidjavajackson.Callback;
import io.atomicbits.raml10.dsl.androidjavajackson.Response;
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

        restActions = new ArrayList<>();
        restActions.add(new RestRequestTestOk());
        restActions.add(new RestRequestTestError());

        adapter = new MySimpleArrayAdapter(this, restActions);

        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);

        runButton = findViewById(R.id.navigation_run);

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

    }

    private void clearStatus() {
        for (RestAction action : restActions) {
            action.setSuccessful(null);
        }
        adapter.notifyDataSetChanged();
    }

    private void startAllTests() {
        runButton.setEnabled(false);
        clearStatus();
        toRun = new ArrayList<>();
        toRun.addAll(restActions);
        runNext();
    }

    private void runNext() {
        if (!toRun.isEmpty()) {
            final RestAction action = toRun.remove(0);
            action.call(new Callback<Object>() {
                @Override
                public void onFailure(Throwable t) {
                    action.setSuccessful(false);
                    adapter.notifyDataSetChanged();
                    runNext();
                }

                @Override
                public void onNokResponse(Response<String> response) {
                    action.setSuccessful(false);
                    adapter.notifyDataSetChanged();
                    runNext();
                }

                @Override
                public void onOkResponse(Response<Object> response) {
                    action.setSuccessful(true);
                    adapter.notifyDataSetChanged();
                    runNext();
                }
            });
        } else {
            runButton.setEnabled(true);
        }
    }

    private void reset() {
        this.toRun = new ArrayList<>();
        clearStatus();
        runButton.setEnabled(true);
    }

}
