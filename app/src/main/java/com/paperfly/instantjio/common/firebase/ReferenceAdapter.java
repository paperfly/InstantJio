package com.paperfly.instantjio.common.firebase;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.firebase.client.ChildEventListener;
import com.firebase.client.Query;

import java.util.ArrayList;

public abstract class ReferenceAdapter<ViewHolder extends RecyclerView.ViewHolder, T> extends RecyclerView.Adapter<ViewHolder> {
    private static final String TAG = ReferenceAdapter.class.getCanonicalName();
    protected Query mFirstRef;
    protected Query mSecondRef;
    protected Class<T> mItemClass;
    protected ItemEventListener<T> mItemListener;
    protected ArrayList<T> mItems;
    protected ArrayList<String> mKeys;
    protected ChildEventListener mListener;

    public ReferenceAdapter(Query firstRef, Query secondRef, Class<T> itemClass, ItemEventListener<T> itemListener) {
        mFirstRef = firstRef;
        mSecondRef = secondRef;
        mItemClass = itemClass;
        mItemListener = itemListener;
        mItems = new ArrayList<>();
        mKeys = new ArrayList<>();
    }

    public void enable() {
        mFirstRef.addChildEventListener(mListener);
    }

    public void disable() {
        mFirstRef.removeEventListener(mListener);
    }

    @Override
    public abstract ViewHolder onCreateViewHolder(ViewGroup parent, int viewType);

    @Override
    public abstract void onBindViewHolder(ViewHolder holder, int position);

    @Override
    public int getItemCount() {
        return (mItems != null) ? mItems.size() : 0;
    }

    public ArrayList<T> getItems() {
        return mItems;
    }

    public ArrayList<String> getKeys() {
        return mKeys;
    }

    public T getItem(int position) {
        return mItems.get(position);
    }

    public int getPositionForItem(T item) {
        return mItems != null && mItems.size() > 0 ? mItems.indexOf(item) : -1;
    }

    public boolean contains(T item) {
        return mItems != null && mItems.contains(item);
    }

    protected interface ItemEventListener<T> {
        void itemAdded(T item, String key, int position);

        void itemChanged(T oldItem, T newItem, String key, int position);

        void itemRemoved(T item, String key, int position);

        void itemMoved(T item, String key, int oldPosition, int newPosition);
    }
}
