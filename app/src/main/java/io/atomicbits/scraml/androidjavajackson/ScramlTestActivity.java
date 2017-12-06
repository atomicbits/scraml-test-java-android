package io.atomicbits.scraml.androidjavajackson;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import io.atomicbits.raml10.RamlTestClient;


public class ScramlTestActivity extends AppCompatActivity {

    private TextView mTextMessage; // remove

    // Get ListView object from xml
    ListView listView;

    protected Object mActionMode;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home); // remove
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard); // remove
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications); // remove
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scraml_test);

        mTextMessage = (TextView) findViewById(R.id.message); // remove

        listView = (ListView) findViewById(R.id.listView);

        // mActionMode = new MyListActivity();

        String[] values = new String[] { "Android", "iPhone", "WindowsMobile",
                "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
                "Linux", "OS/2" };
        MySimpleArrayAdapter adapter = new MySimpleArrayAdapter(this, values);

        listView.setAdapter(new MySimpleArrayAdapter(this, values));

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

    }

}
