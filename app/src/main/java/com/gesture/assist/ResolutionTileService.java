package com.gesture.assist;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.TileService;
import android.widget.Toast;

public class ResolutionTileService extends TileService {
    private boolean isActive = false;
    private Handler handler = new Handler(Looper.getMainLooper());

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

        if (isActive) {
            Toast.makeText(this, "🥵 BẬT: Kéo giãn 1.7x + Khuếch đại cử chỉ", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "🥶 TẮT: Reset màn hình + Tắt khuếch đại", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTile(boolean active) {
        if (active) {
            getQsTile().setState(1);
            getQsTile().setLabel("🥵 ON");
        } else {
            getQsTile().setState(0);
            getQsTile().setLabel("🥶 OFF");
        }
        getQsTile().updateTile();
    }
}
