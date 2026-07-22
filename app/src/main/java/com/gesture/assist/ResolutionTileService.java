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

        if (isActive) {
            Intent intent = new Intent("com.gesture.assist.EXECUTE");
            intent.putExtra("mode", "enable");
            sendBroadcast(intent);
            Toast.makeText(this, "🥵 Đang Quay Tay Cực Mạnh 💦", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent("com.gesture.assist.EXECUTE");
            intent.putExtra("mode", "disable");
            sendBroadcast(intent);
            Toast.makeText(this, "🥶 Đéo Nổi Rồi Toạc BQĐ mất 😵‍💫", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTile(boolean active) {
        if (active) {
            getQsTile().setState(TileService.STATE_ACTIVE);
            getQsTile().setLabel("🥵 Đang Quay Tay Cực Mạnh💦");
        } else {
            getQsTile().setState(TileService.STATE_INACTIVE);
            getQsTile().setLabel("🥶 Đéo Nổi Rồi Toạc BQĐ mất 😵‍💫");
        }
        getQsTile().updateTile();
    }
}
