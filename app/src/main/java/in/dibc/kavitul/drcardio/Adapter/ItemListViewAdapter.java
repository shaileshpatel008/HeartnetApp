package in.dibc.kavitul.drcardio.Adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;

import in.dibc.kavitul.drcardio.R;

public class ItemListViewAdapter extends ArrayAdapter<String> implements Filterable {

    // declaring our ArrayList of items
    private ArrayList<String> objects;
    private ArrayList<String> objectsFiltered;

    Context mContext;

    /* here we must override the constructor for ArrayAdapter
     * the only variable we care about now is ArrayList<Item> objects,
     * because it is the list of objects we want to display.
     */
    public ItemListViewAdapter(Context context, int textViewResourceId, ArrayList<String> objects) {
        super(context, textViewResourceId, objects);
        this.objects = objects;
        this.objectsFiltered = objects;
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return objectsFiltered.size();
    }


    /*
     * we are overriding the getView method here - this is what defines how each
     * list item will look.
     */
    public View getView(int position, View convertView, ViewGroup parent){

        // assign the view we are converting to a local variable
        View v = convertView;

        // first check to see if the view is null. if so, we have to inflate it.
        // to inflate it basically means to render, or show, the view.
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.item_list_name, null);
        }

        /*
         * Recall that the variable position is sent in as an argument to this method.
         * The variable simply refers to the position of the current object in the list. (The ArrayAdapter
         * iterates through the list we sent it)
         *
         * Therefore, i refers to the current Item object.
         */
        String i = objectsFiltered.get(position);

        if (i != null) {

            // This is how you obtain a reference to the TextViews.
            // These TextViews are created in the XML files we defined.

            TextView tt = v.findViewById(R.id.tvTitle);
            LinearLayout llReported = v.findViewById(R.id.llItem);


            //llItem.setBackground(mContext.getResources().getDrawable(R.drawable.btn_roundedrect_primary_light));

            // check to see if each individual textview is null.
            // if not, assign some text!
            if (tt != null){
//                String kept = i.substring( 0, i.indexOf("$$"));
//                String remainder = i.substring(i.indexOf(",")+1, i.length());
//                Log.v("First :: ", kept);
//                Log.v("Second :: ", remainder);
                tt.setText(""+i);
            }
        }

        // the view must be returned to our activity
        return v;

    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                FilterResults filterResults = new FilterResults();
                if(constraint == null || constraint.length() == 0){
                    filterResults.count = objectsFiltered.size();
                    filterResults.values = objects;

                } else{
                    ArrayList<String> resultsModel = new ArrayList<>();
                    String searchStr = constraint.toString().toLowerCase();

                    for(String itemsModel:objects){
                        if(itemsModel.toLowerCase().contains(searchStr.toLowerCase())){
                            resultsModel.add(itemsModel);
                        }
                        filterResults.count = resultsModel.size();
                        filterResults.values = resultsModel;
                    }
                }

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                objectsFiltered = (ArrayList<String>) results.values;
                notifyDataSetChanged();

            }
        };
        return filter;
    }
}