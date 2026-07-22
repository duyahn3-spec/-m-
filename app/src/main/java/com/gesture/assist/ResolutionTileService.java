package com.gesture.assist;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.TileService;
import android.widget.Toast;

public class ResolutionTileService extends TileService {
    private boolean isActive = false;

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile(false);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile(isActive);
    }

    @Override
    public void onClick() {
        super.onClick();
        isActive = !isActive;
        updateTile(isActive);

        Intent intent = new Intent("com.gesture.assist.TOGGLE_ALL");
        intent.putExtra("enable", isActive);
        sendBroadcast(intent);

        Toast.makeText(this, isActive ? "🔥 BẬT khuếch đại 100x" : "🧊 TẮT khuếch đại", Toast.LENGTH_SHORT).show();
    }

    private void updateTile(boolean active) {
        if (active) {
            getQsTile().setState(1);
            getQsTile().setLabel("🥵 100x ON");
        } else {
            getQsTile().setState(0);
            getQsTile().setLabel("🥶 OFF");
        }
        getQsTile().updateTile();
    }
}
