package com.google.firebase.udacity.friendlychat;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;


import java.util.List;

public class MessageAdapter extends ArrayAdapter<FriendlyMessage> {
    public MessageAdapter(Context context, int resource, List<FriendlyMessage> objects) {
        super(context, resource, objects);
        this.mContext = context;
    }

    private Context mContext;
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder mainViewHolder = null;

        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_message, parent, false);
            final ViewHolder viewHolder = new ViewHolder();
            viewHolder.photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
            viewHolder.messageTextView = (TextView) convertView.findViewById(R.id.messageTextView);
            viewHolder.authorTextView = (TextView) convertView.findViewById(R.id.nameTextView);
            viewHolder.button_speak = (Button) convertView.findViewById(R.id.button_speak);
            viewHolder.button_speak.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String text = viewHolder.messageTextView.getText().toString();
                    if(mContext instanceof MainActivity){
                        Log.println(1, "Button Pressed", "Button Pressed");
                        Toast.makeText(getContext(), "Button was clicked for list item "+ position, Toast.LENGTH_SHORT).show();
                        ((MainActivity)mContext).speak(text);
                    }
                }
            });

            convertView.setTag(viewHolder);
            FriendlyMessage message = getItem(position);

            boolean isPhoto = message.getPhotoUrl() != null;
            if (isPhoto) {
                viewHolder.messageTextView.setVisibility(View.GONE);
                viewHolder.photoImageView.setVisibility(View.VISIBLE);
                Glide.with(viewHolder.photoImageView.getContext())
                        .load(message.getPhotoUrl())
                        .into(viewHolder.photoImageView);
            } else {
                viewHolder.messageTextView.setVisibility(View.VISIBLE);
                viewHolder.photoImageView.setVisibility(View.GONE);
                viewHolder.messageTextView.setText(message.getText());
            }
            viewHolder.authorTextView.setText(message.getName());
        }
        else {
            mainViewHolder = (ViewHolder) convertView.getTag();
            FriendlyMessage message = getItem(position);
            mainViewHolder.messageTextView.setText(message.getText());
            mainViewHolder.authorTextView.setText(message.getName());

        }
        return convertView;
    }


}

