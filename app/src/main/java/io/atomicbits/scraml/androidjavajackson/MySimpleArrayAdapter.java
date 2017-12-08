package io.atomicbits.scraml.androidjavajackson;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import io.atomicbits.scraml.androidjavajackson.restaction.RestAction;

/**
 * Created by peter on 6/12/17.
 */

public class MySimpleArrayAdapter extends ArrayAdapter<RestAction> {

    private final Context context;
    private final List<RestAction> values;

    public MySimpleArrayAdapter(Context context, List<RestAction> values) {
        super(context, R.layout.rowlayout, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.label);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

        RestAction action = values.get(position);
        textView.setText(action.getName());

        if (action.isSuccessful() == null) {
            imageView.setImageResource(R.drawable.ic_check_box_outline_blank_black_24px);
        } else {
            if (action.isSuccessful()) {
                imageView.setColorFilter(getContext().getResources().getColor(R.color.darkgreen));
                imageView.setImageResource(R.drawable.ic_check_box_black_24px);
            } else {
                imageView.setColorFilter(getContext().getResources().getColor(R.color.darkred));
                imageView.setImageResource(R.drawable.ic_indeterminate_check_box_black_24px);
            }
        }

        return rowView;
    }
}
