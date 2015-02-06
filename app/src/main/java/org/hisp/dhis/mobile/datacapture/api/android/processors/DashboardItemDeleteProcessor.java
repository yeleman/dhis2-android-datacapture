package org.hisp.dhis.mobile.datacapture.api.android.processors;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.hisp.dhis.mobile.datacapture.api.APIException;
import org.hisp.dhis.mobile.datacapture.api.android.events.DashboardItemDeleteEvent;
import org.hisp.dhis.mobile.datacapture.api.android.events.OnDashboardItemDeleteEvent;
import org.hisp.dhis.mobile.datacapture.api.android.handlers.DashboardHandler;
import org.hisp.dhis.mobile.datacapture.api.android.handlers.DashboardItemHandler;
import org.hisp.dhis.mobile.datacapture.api.android.models.DBItemHolder;
import org.hisp.dhis.mobile.datacapture.api.android.models.ResponseHolder;
import org.hisp.dhis.mobile.datacapture.api.android.models.State;
import org.hisp.dhis.mobile.datacapture.api.managers.DHISManager;
import org.hisp.dhis.mobile.datacapture.api.models.Dashboard;
import org.hisp.dhis.mobile.datacapture.api.models.DashboardItem;
import org.hisp.dhis.mobile.datacapture.api.network.ApiRequestCallback;
import org.hisp.dhis.mobile.datacapture.api.network.Response;
import org.hisp.dhis.mobile.datacapture.io.DBContract.DashboardColumns;
import org.hisp.dhis.mobile.datacapture.io.DBContract.DashboardItemColumns;

public class DashboardItemDeleteProcessor extends AbsProcessor<DashboardItemDeleteEvent, OnDashboardItemDeleteEvent> {
    public DashboardItemDeleteProcessor(Context context, DashboardItemDeleteEvent event) {
        super(context, event);
    }

    @Override
    public OnDashboardItemDeleteEvent process() {
        final DBItemHolder<Dashboard> mDashboard = readDashboard();
        final DBItemHolder<DashboardItem> mDashboardItem = readDashboardItem();
        final ResponseHolder<String> holder = new ResponseHolder<>();
        final OnDashboardItemDeleteEvent event = new OnDashboardItemDeleteEvent();

        String dashboardId = mDashboard.getItem().getId();
        String dashboardItemId = mDashboardItem.getItem().getId();

        updateDashboardItemState(State.DELETING);
        DHISManager.getInstance().deleteItemFromDashboard(new ApiRequestCallback<String>() {
            @Override
            public void onSuccess(Response response, String s) {
                holder.setResponse(response);
                holder.setItem(s);
                deleteDashboardItem();
            }

            @Override
            public void onFailure(APIException e) {
                holder.setException(e);
            }
        }, dashboardId, dashboardItemId);

        event.setResponseHolder(holder);
        return event;
    }

    private DBItemHolder<DashboardItem> readDashboardItem() {
        Uri uri = ContentUris.withAppendedId(
                DashboardItemColumns.CONTENT_URI, getEvent().getDashboardItemDbId());
        Cursor cursor = getContext().getContentResolver().query(
                uri, DashboardItemHandler.PROJECTION, null, null, null
        );

        DBItemHolder<DashboardItem> dbItem = null;
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            dbItem = DashboardItemHandler.fromCursor(cursor);
            cursor.close();
        }

        return dbItem;
    }

    private DBItemHolder<Dashboard> readDashboard() {
        Uri uri = ContentUris.withAppendedId(
                DashboardColumns.CONTENT_URI, getEvent().getDashboardDbId());
        Cursor cursor = getContext().getContentResolver().query(
                uri, DashboardHandler.PROJECTION, null, null, null
        );

        DBItemHolder<Dashboard> dbItem = null;
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            dbItem = DashboardHandler.fromCursor(cursor);
            cursor.close();
        }

        return dbItem;
    }

    private void deleteDashboardItem() {
        Uri uri = ContentUris.withAppendedId(
                DashboardItemColumns.CONTENT_URI, getEvent().getDashboardItemDbId());
        getContext().getContentResolver().delete(uri, null, null);
    }

    private void updateDashboardItemState(State state) {
        Uri uri = ContentUris.withAppendedId(
                DashboardItemColumns.CONTENT_URI, getEvent().getDashboardItemDbId());
        ContentValues values = new ContentValues();
        values.put(DashboardItemColumns.STATE, state.toString());
        getContext().getContentResolver().update(uri, values, null, null);
    }
}
