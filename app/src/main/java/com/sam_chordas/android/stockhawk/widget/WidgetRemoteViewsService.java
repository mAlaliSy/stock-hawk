package com.sam_chordas.android.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.DetailsActivity;

/**
 * Created by mohammad on 17/08/16.
 */
public class WidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new MyRemoteViewFactory(this);
    }




    static class MyRemoteViewFactory implements RemoteViewsFactory{

        Cursor data ;
        Context mContext ;

        MyRemoteViewFactory(Context context){
            this.mContext = context;
        }

        @Override
        public void onCreate() {

        }

        @Override
        public void onDataSetChanged() {
            if (data != null)
                data.close();

            final long identityToken = Binder.clearCallingIdentity();

            data = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                    QuoteColumns.ISCURRENT + " = ?",
                    new String[]{"1"},
                    null);

            Binder.restoreCallingIdentity(identityToken);


        }

        @Override
        public void onDestroy() {
            if (data != null ){
                data.close();
                data = null;
            }
        }

        @Override
        public int getCount() {
            return (data == null ? 0 : data.getCount());
        }

        @Override
        public RemoteViews getViewAt(int position) {


            if (position == AdapterView.INVALID_POSITION ||
                    data == null || !data.moveToPosition(position)) {
                return null;
            }

            RemoteViews remoteViews = new RemoteViews(mContext.getPackageName() , R.layout.list_item_quote);

            remoteViews.setTextViewText(R.id.bid_price , data.getString(data.getColumnIndex("bid_price")));

            remoteViews.setTextViewText(R.id.stock_symbol , data.getString(data.getColumnIndex("symbol")));



            if (Utils.showPercent)
                remoteViews.setTextViewText(R.id.change ,data.getString(data.getColumnIndex("percent_change")));
            else
                remoteViews.setTextViewText(R.id.change ,data.getString(data.getColumnIndex("change")));



            if (data.getInt(data.getColumnIndex("is_up")) == 1) {
                remoteViews.setInt(R.id.change , "setBackgroundResource" , R.drawable.percent_change_pill_green);
            }else {
                remoteViews.setInt(R.id.change , "setBackgroundResource" , R.drawable.percent_change_pill_red);
            }

        Intent fillIntent = new Intent(mContext , DetailsActivity.class);
        fillIntent.putExtra("symbol" ,  data.getString(data.getColumnIndex("symbol")));

        remoteViews.setOnClickFillInIntent(R.id.list_item , fillIntent);

        return remoteViews;
    }



        @Override
        public RemoteViews getLoadingView() {
            return new RemoteViews(mContext.getPackageName(), R.layout.list_item_quote);
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            if (data.moveToPosition(position))
                return data.getLong(0);
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }


    }

}
