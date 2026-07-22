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
    public void onClick() {
        super.onClick();
        isActive = !isActive;
        updateTile(isActive);

        if (isActive) {
            // Bật kéo giãn 1.7x và tối ưu
            Intent intent = new Intent("com.gesture.assist.EXECUTE");
            intent.putExtra("mode", "enable");
            sendBroadcast(intent);
            Toast.makeText(this, "✅ Đã kéo giãn 1.7x & tối ưu CPU", Toast.LENGTH_SHORT).show();
        } else {
            // Reset độ phân giải về mặc định
            Intent intent = new Intent("com.gesture.assist.EXECUTE");
            intent.putExtra("mode", "disable");
            sendBroadcast(intent);
            Toast.makeText(this, "🔁 Đã reset độ phân giải", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTile(boolean active) {
        if (active) {
            getQsTile().setState(TileService.STATE_ACTIVE);
            getQsTile().setLabel("1.7x ON");
        } else {
            getQsTile().setState(TileService.STATE_INACTIVE);
            getQsTile().setLabel("1.7x OFF");
        }
        getQsTile().updateTile();
    }
}
