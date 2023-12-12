package com.geanmaidana.image2pdf;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;

public class ImageReorderCallback extends ItemTouchHelper.Callback {
    private ArrayList<Uri> imageUris;
    private ImageAdapter adapter;
    private int fromPosition = -1;
    private int toPosition = -1;
    private static final int SCROLL_EDGE_DISTANCE = 100;
    private RecyclerView recyclerView;

    public ImageReorderCallback(ArrayList<Uri> imageUris, ImageAdapter adapter, RecyclerView
            recyclerView) {
        this.imageUris = imageUris;
        this.adapter = adapter;
        this.recyclerView = recyclerView;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                ItemTouchHelper.START | ItemTouchHelper.END, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull
    RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {

        if (fromPosition == -1)
            fromPosition = viewHolder.getAdapterPosition();

        toPosition = target.getAdapterPosition();
        return true;
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        if (fromPosition != -1 && toPosition != -1 && fromPosition != toPosition) {
            // Save the current scroll position
            int scrollPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();

            Collections.swap(imageUris, fromPosition, toPosition);
            adapter.notifyItemChanged(fromPosition);
            adapter.notifyItemChanged(toPosition);

            // Restore the scroll position
            recyclerView.scrollToPosition(scrollPosition);
        }
        fromPosition = -1;
        toPosition = -1;
    }


    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Do nothing
    }
}