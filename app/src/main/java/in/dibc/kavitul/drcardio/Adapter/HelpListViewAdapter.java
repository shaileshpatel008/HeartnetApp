package in.dibc.kavitul.drcardio.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import in.dibc.kavitul.drcardio.R;
import in.dibc.kavitul.drcardio.Model.Helpmodel;

public class HelpListViewAdapter extends BaseAdapter {
    /*Help Screen Adpter to Set Help Title and Help Description*/
    Context mContext;
    LayoutInflater inflater;
    private List<Helpmodel> description = null;
    private ArrayList<Helpmodel> arraylist;

    public HelpListViewAdapter(Context context, List<Helpmodel> description) {
        mContext = context;
        this.description = description;
        inflater = LayoutInflater.from(mContext);
        this.arraylist = new ArrayList<Helpmodel>();
        this.arraylist.addAll(description);
    }

    public class ViewHolder {
        TextView name;
        TextView desc;
    }

    @Override
    public int getCount() {
        return description.size();
    }

    @Override
    public Helpmodel getItem(int position) {
        return description.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View view, ViewGroup parent) {
        final ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(R.layout.act_list_item, null);
            // Locate the TextViews in listview_item.xml
            holder.name = view.findViewById(R.id.txt_help_title_heder);
            holder.desc = view.findViewById(R.id.txt_help_short_description);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        // Set the results into TextViews
        holder.name.setText(description.get(position).getTitle());
        holder.desc.setText(description.get(position).getDescription());
        return view;
    }

    // Filter Class
    public void filter(String charText) {
        charText = charText.toLowerCase(Locale.getDefault());
        description.clear();
        if (charText.length() == 0) {
            description.addAll(arraylist);
        } else {
            for (Helpmodel wp : arraylist) {
                if (wp.getTitle().toLowerCase(Locale.getDefault()).contains(charText)) {
                    description.add(wp);
                }
            }
        }
        notifyDataSetChanged();
    }

}