package com.sgo.orimakardaya.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.sgo.orimakardaya.Beans.commentModel;
import com.sgo.orimakardaya.R;

import java.util.List;

/**
 * Created by thinkpad on 4/20/2015.
 */
public class TimelineCommentAdapter extends BaseAdapter {
    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    private List<commentModel> comment;
    private LayoutInflater mInflater;

    public TimelineCommentAdapter(Context context, List<commentModel> comment) {
        mInflater = LayoutInflater.from(context);
        this.comment = comment;
    }

    @Override
    public int getCount() {
        return comment.size();
    }

    @Override
    public Object getItem(int position) {
        return comment.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder holder;
        if(convertView == null) {
            view = mInflater.inflate(R.layout.list_timeline_comment_item, parent, false);
            holder = new ViewHolder();
            holder.name = (TextView)view.findViewById(R.id.txt_name);
            holder.message = (TextView)view.findViewById(R.id.txt_message);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder)view.getTag();
        }

        holder.name.setText(comment.get(position).getFrom_name());
        holder.message.setText(comment.get(position).getReply());

        return view;
    }

    private class ViewHolder {
        public TextView name, message;
    }
}
