package com.sgo.orimakardaya.adapter;/*
  Created by Administrator on 11/24/2014.
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.securepreferences.SecurePreferences;
import com.sgo.orimakardaya.Beans.listTimeLineModel;
import com.sgo.orimakardaya.R;
import com.sgo.orimakardaya.coreclass.*;
import com.squareup.picasso.Picasso;
import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class TimeLineRecycleAdapter extends RecyclerView.Adapter<TimeLineRecycleAdapter.SimpleHolder>{

    private List<listTimeLineModel> mData;
    private int rowLayout;
    private Context mContext;
    OnItemClickListener mItemClickListener;
    SecurePreferences sp;
    String user_id,accessKey;

    public TimeLineRecycleAdapter(List<listTimeLineModel> _mData, int _rowLayout, Context _mContext){
        mData = _mData;
        rowLayout = _rowLayout;
        mContext = _mContext;

        sp = CustomSecurePref.getInstance().getmSecurePrefs();
        user_id = sp.getString(DefineValue.USERID_PHONE,"");
        accessKey = sp.getString(DefineValue.ACCESS_KEY,"");
    }

    @Override
    public SimpleHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(rowLayout,viewGroup,false);
        return new SimpleHolder(v);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(final SimpleHolder simpleHolder, int i) {
        final listTimeLineModel _data = mData.get(i);

        String string_date = _data.getDatetime();

        /*SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d = null;
        try {
            d = f.parse(string_date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long long_date = 0;
        if (d != null) {
            long_date = d.getTime();
        }

        String period = PeriodTime.getTimeAgo(long_date, mContext);
*/
        PrettyTime p = new PrettyTime(new Locale(DefineValue.sDefSystemLanguage));
        Date time1 = DateTimeFormat.convertCustomDate(string_date);
        String period = p.formatDuration(time1);

//        String tx_status = "";
//        if(_data.getTx_status().equals("S")) tx_status = "Paid";
//        else tx_status = "Pending";

        simpleHolder.fromId.setText(_data.getOwner());
        simpleHolder.toId.setText(_data.getWith());
        simpleHolder.messageTransaction.setText(_data.getPost());
        simpleHolder.amount.setText(_data.getCcy_id() + " " + CurrencyFormat.format(_data.getAmount()));
        simpleHolder.dateTime.setText(period);
        simpleHolder.status.setText(_data.getTypecaption());

        setImageProfPic(_data.getOwner_profile_picture(), simpleHolder.iconPicture);
        setImageProfPic(_data.getWith_profile_picture(), simpleHolder.iconPictureRight);

        simpleHolder.likeCount.setText(_data.getNumlikes());;
        simpleHolder.commentCount.setText(_data.getNumcomments());

        if(_data.getComment_id_2().equals("")) {
            simpleHolder.layoutComment1.setVisibility(View.GONE);
        }
        else {
            simpleHolder.layoutComment1.setVisibility(View.VISIBLE);
            simpleHolder.nameComment1.setText(_data.getFrom_name_2());
            simpleHolder.textComment1.setText(_data.getReply_2());
            setImageProfPic(_data.getFrom_profile_picture_2(), simpleHolder.iconPictureComment1);
        }

        if(_data.getComment_id_1().equals("")) {
            simpleHolder.layoutComment2.setVisibility(View.GONE);
        }
        else {
            simpleHolder.layoutComment2.setVisibility(View.VISIBLE);
            simpleHolder.nameComment2.setText(_data.getFrom_name_1());
            simpleHolder.textComment2.setText(_data.getReply_1());
            setImageProfPic(_data.getFrom_profile_picture_1(), simpleHolder.iconPictureComment2);
        }

        String comments = _data.getComments();
        if(comments.equals("")) {
            simpleHolder.layoutComment.setVisibility(View.GONE);
        }
        else simpleHolder.layoutComment.setVisibility(View.VISIBLE);

        final String isLike = _data.getIsLike();
        if(isLike.equals("0")) {
            simpleHolder.imageLove.setImageResource(R.drawable.ic_like_inactive);
        }
        else {
            simpleHolder.imageLove.setImageResource(R.drawable.ic_like_active);
        }

        final int timeline_id = _data.getTimeline_id();

        simpleHolder.imageLove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if(InetHandler.isNetworkAvailable(mContext)) {
                    v.setEnabled(false);
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            v.setEnabled(true);
                        }
                    }, 3000);

                    if (isLike.equals("1")) {
                        final String jumlahLike = Integer.toString(Integer.parseInt(simpleHolder.likeCount.getText().toString()) - 1);
                        simpleHolder.imageLove.setImageResource(R.drawable.ic_like_inactive);
                        simpleHolder.likeCount.setText(jumlahLike);
                        listTimeLineModel.updateNumlikes(jumlahLike, timeline_id);
                        listTimeLineModel.updateIsLike("0", timeline_id);

                        final Handler mHandler = new Handler();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                String likes = _data.getLikes();
                                try {
                                    JSONArray mArrayLikes = new JSONArray(likes);
                                    for (int i = 0; i < mArrayLikes.length(); i++) {
                                        String from = mArrayLikes.getJSONObject(i).getString(WebParams.FROM);
                                        if (user_id.equals(from)) {
                                            String like_id = mArrayLikes.getJSONObject(i).getString(WebParams.ID);
                                            String to = mArrayLikes.getJSONObject(i).getString(WebParams.TO);
                                            removeLike(like_id, user_id, to, Integer.toString(timeline_id), jumlahLike);
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else if (isLike.equals("0")) {
                        final String jumlahLike = Integer.toString(Integer.parseInt(simpleHolder.likeCount.getText().toString()) + 1);
                        simpleHolder.imageLove.setImageResource(R.drawable.ic_like_active);
                        simpleHolder.likeCount.setText(jumlahLike);
                        listTimeLineModel.updateNumlikes(jumlahLike, timeline_id);
                        listTimeLineModel.updateIsLike("1", timeline_id);

                        final Handler mHandler = new Handler();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                addLike(Integer.toString(timeline_id), _data.getOwner_id(), jumlahLike);
                            }
                        });
                    }

                    swap(listTimeLineModel.getAll());
                }
            }
        });
    }



    public void setImageProfPic(String _data, QuickContactBadge _holder){
        /*
        float density = getResources().getDisplayMetrics().density;
        String _url_profpic;

        if(density <= 1) _url_profpic = sp.getString(CoreApp.IMG_SMALL_URL, null);
        else if(density < 2) _url_profpic = sp.getString(CoreApp.IMG_MEDIUM_URL, null);
        else _url_profpic = sp.getString(CoreApp.IMG_LARGE_URL, null);

        Log.wtf("url prof pic", _url_profpic);

        */

        Bitmap bm = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.user_unknown_menu);
        RoundImageTransformation roundedImage = new RoundImageTransformation(bm);

        Picasso mPic;
        if(MyApiClient.PROD_FLAG_ADDRESS)
            mPic = MyPicasso.getImageLoader(mContext);
        else
            mPic= Picasso.with(mContext);

        if(_data.equals("") || _data.isEmpty()){
            mPic.load(R.drawable.user_unknown_menu)
                .error(roundedImage)
                .fit().centerInside()
                .placeholder(R.anim.progress_animation)
                .transform(new RoundImageTransformation(mContext))
                .into(_holder);
        }
        else {
            mPic.load(_data)
                .error(roundedImage)
                .fit()
                .centerCrop()
                .placeholder(R.anim.progress_animation)
                .transform(new RoundImageTransformation(mContext))
                .into(_holder);
        }
    }

    public void swap(List<listTimeLineModel> datas){
        mData.clear();
        mData.addAll(datas);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public class SimpleHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public LinearLayout layoutComment, layoutComment1, layoutComment2;
        public TextView fromId,toId,messageTransaction,amount,dateTime, status, comment, nameComment1, textComment1, nameComment2, textComment2,
                        likeCount, commentCount;
        public ImageView imageLove;
        public QuickContactBadge iconPicture, iconPictureRight, iconPictureComment1, iconPictureComment2;

        public SimpleHolder(View itemView) {
            super(itemView);
            fromId = (TextView) itemView.findViewById(R.id.from_id);
            toId = (TextView)itemView.findViewById(R.id.to_id);
            messageTransaction = (TextView)itemView.findViewById(R.id.message_transaction);
            amount = (TextView)itemView.findViewById(R.id.amount);
            dateTime = (TextView)itemView.findViewById(R.id.datetime);
            iconPicture = (QuickContactBadge)itemView.findViewById(R.id.icon_picture);
            iconPictureRight = (QuickContactBadge)itemView.findViewById(R.id.icon_picture_right);
            likeCount = (TextView)itemView.findViewById(R.id.like_count);
            commentCount = (TextView)itemView.findViewById(R.id.comment_count);
            iconPictureComment1 = (QuickContactBadge)itemView.findViewById(R.id.icon_picture_comment1);
            iconPictureComment2 = (QuickContactBadge)itemView.findViewById(R.id.icon_picture_comment2);
            layoutComment = (LinearLayout)itemView.findViewById(R.id.layout_comment);
            layoutComment1 = (LinearLayout)itemView.findViewById(R.id.layout_comment1);
            layoutComment2 = (LinearLayout)itemView.findViewById(R.id.layout_comment2);
            nameComment1 = (TextView)itemView.findViewById(R.id.name_comment1);
            nameComment2 = (TextView)itemView.findViewById(R.id.name_comment2);
            textComment1 = (TextView)itemView.findViewById(R.id.text_comment1);
            textComment2 = (TextView)itemView.findViewById(R.id.text_comment2);
            status = (TextView)itemView.findViewById(R.id.status);
            imageLove = (ImageView)itemView.findViewById(R.id.image_love);
            comment = (TextView) itemView.findViewById(R.id.value_comment);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(mItemClickListener != null) {
                mItemClickListener.onItemClick(v, getPosition());
            }
        }

    }

    public interface OnItemClickListener {
        public void onItemClick(View view, int position);
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }


    public void addLike(final String post_id, String from_id, final String jumlahLike) {
        try {
            RequestParams params = MyApiClient.getSignatureWithParams(MyApiClient.COMM_ID,MyApiClient.LINK_ADD_LIKE,
                    user_id,accessKey);
            params.put(WebParams.POST_ID, post_id);
            params.put(WebParams.FROM, user_id);
            params.put(WebParams.TO, from_id);
            params.put(WebParams.DATETIME, DateTimeFormat.getCurrentDate());
            params.put(WebParams.USER_ID, user_id);
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);

            Timber.d("isi params add like:"+params.toString());

            MyApiClient.sentAddLike(params, new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        String code = response.getString(WebParams.ERROR_CODE);
                        String count = response.getString(WebParams.COUNT);

                        if (code.equals(WebParams.SUCCESS_CODE) && !count.equals("0")) {
                            Timber.d("isi params add like:" + response.toString());

                            String data_likes = response.getString(WebParams.DATA_LIKES);
                            listTimeLineModel.updateLikes(data_likes, Integer.parseInt(post_id));
                        }
                        else if(code.equals(WebParams.NO_DATA_CODE)) {
                            Timber.d("isi params add like:"+response.toString());

                            String data_likes = response.getString(WebParams.DATA_LIKES);
                            listTimeLineModel.updateLikes(data_likes, Integer.parseInt(post_id));
                        }
                        else {
                            Timber.d("isi error add like:"+response.toString());

                            listTimeLineModel.updateNumlikes(Integer.toString(Integer.parseInt(jumlahLike)-1), Integer.parseInt(post_id));
                            listTimeLineModel.updateIsLike("0", Integer.parseInt(post_id));
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    failure(throwable);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    failure(throwable);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    failure(throwable);
                }

                private void failure(Throwable throwable){
                    if(MyApiClient.PROD_FAILURE_FLAG)
                        Toast.makeText(mContext, mContext.getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(mContext, throwable.toString(), Toast.LENGTH_SHORT).show();
                    Timber.w("Error Koneksi add like:"+throwable.toString());
                }
            });
        }
        catch(Exception e){
            Timber.d("httpclient:"+e.getMessage());
        }
    }


    public void removeLike(String like_id, String from, String to, final String post_id, final String jumlahLike) {
        try {


            RequestParams params = MyApiClient.getSignatureWithParams(MyApiClient.COMM_ID,MyApiClient.LINK_REMOVE_LIKE,
                    user_id,accessKey);
            params.put(WebParams.LIKE_ID, like_id);
            params.put(WebParams.POST_ID, post_id);
            params.put(WebParams.FROM, from);
            params.put(WebParams.TO, to);
            params.put(WebParams.USER_ID, user_id);
            params.put(WebParams.COMM_ID, MyApiClient.COMM_ID);

            Timber.d("isi params remove like:"+params.toString());

            MyApiClient.sentRemoveLike(params, new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        String code = response.getString(WebParams.ERROR_CODE);
                        String count = response.getString(WebParams.COUNT);

                        if (code.equals(WebParams.SUCCESS_CODE) && !count.equals("0")) {
                            Timber.d("isi params remove like:"+response.toString());

                            String data_likes = response.getString(WebParams.DATA_LIKES);
                            listTimeLineModel.updateLikes(data_likes, Integer.parseInt(post_id));
                        }
                        else if(code.equals(WebParams.NO_DATA_CODE)) {
                            Timber.d("isi params remove like:"+response.toString());

                            String data_likes = response.getString(WebParams.DATA_LIKES);
                            listTimeLineModel.updateLikes(data_likes, Integer.parseInt(post_id));
                        }
                        else {
                            Timber.d("isi error remove like:"+response.toString());

                            listTimeLineModel.updateNumlikes(Integer.toString(Integer.parseInt(jumlahLike)+1), Integer.parseInt(post_id));
                            listTimeLineModel.updateIsLike("1", Integer.parseInt(post_id));
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    failure(throwable);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    failure(throwable);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    failure(throwable);
                }

                private void failure(Throwable throwable){
                    if(MyApiClient.PROD_FAILURE_FLAG)
                        Toast.makeText(mContext, mContext.getString(R.string.network_connection_failure_toast), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(mContext, throwable.toString(), Toast.LENGTH_SHORT).show();

                    Timber.w("Error Koneksi remove like:"+throwable.toString());
                }
            });
        }
        catch(Exception e){
            Timber.d("httpclient:"+ e.getMessage());
        }
    }
}
