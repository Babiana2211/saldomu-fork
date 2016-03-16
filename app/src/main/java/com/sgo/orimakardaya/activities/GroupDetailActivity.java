package com.sgo.orimakardaya.activities;

import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.activeandroid.util.Log;
import com.securepreferences.SecurePreferences;
import com.sgo.orimakardaya.Beans.GroupCommentObject;
import com.sgo.orimakardaya.R;
import com.sgo.orimakardaya.adapter.GroupCommentAdapter;
import com.sgo.orimakardaya.coreclass.*;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import timber.log.Timber;

/**
 * Created by thinkpad on 4/21/2015.
 */
public class GroupDetailActivity extends BaseActivity {

    SecurePreferences sp;
    int RESULT;

    TextView txtSection, txtPay, txtGetPaid, txtDesc, txtDate;
    RoundedQuickContactBadge iconPicture;
    ImageView imageLove, imageSendComment;
    EditText etComment;
    ListView lvComment;
    TextView textLove;

    boolean love;
    ArrayList<String> arrayLove;

    ArrayList<GroupCommentObject> listComment;
    GroupCommentAdapter commentAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InitializeToolbar();
        sp = CustomSecurePref.getInstance().getmSecurePrefs();

        love = false;
        arrayLove = new ArrayList<String>();
        arrayLove.add("Kim");
        arrayLove.add("Lee");
        arrayLove.add("Mr Park");
        arrayLove.add("Sumiati K");
        arrayLove.add("Wicaksono");
        arrayLove.add("Cahyono");

        iconPicture = (RoundedQuickContactBadge) findViewById(R.id.contact_icon_home_group);
        txtPay = (TextView) findViewById(R.id.txtListPay_home_group);
        txtGetPaid = (TextView) findViewById(R.id.txtListGetPaid_home_group);
        txtDesc = (TextView) findViewById(R.id.txtListDesc_home_group);
        txtDate = (TextView) findViewById(R.id.txtListDate_home_group);
        imageLove = (ImageView)findViewById(R.id.image_love);
        imageSendComment = (ImageView)findViewById(R.id.image_comment);
        etComment = (EditText)findViewById(R.id.detail_value_comment);
        textLove = (TextView)findViewById(R.id.detail_value_love);
        lvComment = (ListView)findViewById(R.id.lvComment);

        Bitmap bm = BitmapFactory.decodeResource(this.getResources(), R.drawable.user_unknown_menu);
        RoundImageTransformation roundedImage = new RoundImageTransformation(bm);

        Picasso mPic;
        if(MyApiClient.PROD_FLAG_ADDRESS)
            mPic = MyPicasso.getImageLoader(this);
        else
            mPic= Picasso.with(this);

        Intent i = getIntent();
        if(i != null) {
            String groupName = i.getStringExtra("groupname");
            String pay = i.getStringExtra("pay");
            String getpaid = i.getStringExtra("getpaid");
            String desc = i.getStringExtra("desc");
            String date = i.getStringExtra("date");
            String profpic = i.getStringExtra("profpic");


            if(profpic != null && profpic.equals(""))
                mPic.load(R.drawable.user_unknown_menu)
                    .error(roundedImage)
                    .fit().centerInside()
                    .placeholder(R.anim.progress_animation)
                    .transform(new RoundImageTransformation(this))
                    .into(iconPicture);
            else
                mPic.load(profpic)
                    .error(R.drawable.user_unknown_menu)
                    .placeholder(R.anim.progress_animation)
                    .fit()
                    .centerCrop()
                    .transform(new RoundImageTransformation(this))
                    .into(iconPicture);

            txtPay.setText(pay);
            txtGetPaid.setText(getpaid);
            txtDesc.setText(desc);
            txtDate.setText(date);
        } else {
            mPic.load(R.drawable.user_unknown_menu)
                .error(roundedImage)
                .fit().centerInside()
                .placeholder(R.anim.progress_animation)
                .transform(new RoundImageTransformation(this))
                .into(iconPicture);
        }

        listComment = new ArrayList<GroupCommentObject>();
        GroupCommentObject comment = new GroupCommentObject();
        comment.setName("Lee");
        comment.setMessage("wew");
        listComment.add(comment);

        commentAdapter = new GroupCommentAdapter(this, listComment);
        lvComment.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        lvComment.setAdapter(commentAdapter);

        setLove();

        etComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Timber.d("test comment:" + s.toString());
                if (s.toString().equals("")) {
                    imageSendComment.setImageResource(R.drawable.ic_send_normal);
                } else {
                    imageSendComment.setImageResource(R.drawable.ic_send_clicked);
                }
            }
        });

        commentAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                lvComment.setSelection(commentAdapter.getCount() - 1);
            }
        });

        imageLove.setOnClickListener(imageLikeListener);
        imageSendComment.setOnClickListener(imageCommentListener);

        RESULT = MainPage.RESULT_NORMAL;
    }

    ImageView.OnClickListener imageLikeListener = new ImageView.OnClickListener() {
        @Override
        public void onClick(View v) {
            love = !love;
            String custName = sp.getString(DefineValue.CUST_NAME, getString(R.string.text_strip));
            if(!love) {
                imageLove.setImageResource(R.drawable.ic_like_inactive);

                for(int i = 0 ; i < arrayLove.size() ; i++) {
                    if(arrayLove.get(i).equals(custName)) arrayLove.remove(i);
                }

            }
            else {
                imageLove.setImageResource(R.drawable.ic_like_active);
                arrayLove.add(custName);
            }

            setLove();
        }
    };

    ImageView.OnClickListener imageCommentListener = new ImageView.OnClickListener() {
        @Override
        public void onClick(View v) {
            String message = etComment.getText().toString();
            if(!message.equals("")) {
                GroupCommentObject comment = new GroupCommentObject();
                comment.setName(sp.getString(DefineValue.CUST_NAME, getString(R.string.text_strip)));
                comment.setMessage(message);
                listComment.add(comment);

                commentAdapter.notifyDataSetChanged();
                etComment.setText("");
            }
        }
    };

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_group_detail;
    }

    public void InitializeToolbar(){
        setActionBarIcon(R.drawable.ic_arrow_left);
        setActionBarTitle(getString(R.string.menu_item_group_detail));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setLove() {
        String peopleLove = "";
        for(int i = 0 ; i < arrayLove.size() ; i++) {
            if(i == arrayLove.size()-1) {
                peopleLove += arrayLove.get(i) + " Like this.";
            }
            else {
                peopleLove += arrayLove.get(i) + ", ";
            }
        }
        textLove.setText(peopleLove);
    }
}
