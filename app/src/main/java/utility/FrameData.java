package utility;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class FrameData implements Serializable {
    private static String TAG = FrameData.class.getSimpleName();

    public String type;
    private long videoSendTime;
    public long Sequence;
    public long roundLatency = 0;
    public boolean isIFrame = false;
    public long originalDataSize = 0;
    public long compressedDataSize = 0;
    public long PCtime = 0;

    public byte[] rawFrameData = null;
    public static long sequence = 0;


    public FrameData (boolean isIFrame, byte[] data, int originalSize){
        this.type = "frame_data_from_car";
        this.videoSendTime = System.currentTimeMillis();
        this.isIFrame = isIFrame;

        this.rawFrameData = data;
        // copy the first 65000
        this.compressedDataSize = this.rawFrameData.length;
        this.originalDataSize = originalSize;
        this.Sequence = FrameData.sequence ++;

        double compressRatio = this.compressedDataSize * 100.0/this.originalDataSize;
        //Log.d(TAG, this.Sequence + ", I:" + this.isIFrame + "," + "ratio:" + String.format("%.2f", compressRatio) + ",size:" + this.compressedDataSize);
    }

    public int getDataSize() {
        return rawFrameData.length;
    }


    public long getVideoSendTime() {
        return this.videoSendTime;
    }


/*    public FrameData generateSubFrame(int index) {
        FrameData frame = new FrameData();
// copy the data
        frame.subIndex = index;
        return frame;
    }
*/
    public List<FrameData> split() {
        List<FrameData> res = new ArrayList<FrameData>();
        int len = 60000;
        int subSum = rawFrameData.length/len + (int)(rawFrameData.length%len == 0 ? 0 : 1);
        int totalLen = this.rawFrameData.length;
        for (int i = 0; i < subSum; ++i) {
            int curLen = Math.min(totalLen - len * i, len);
            byte[] newData = new byte[curLen];
            System.arraycopy(this.rawFrameData, i * len, newData, 0, curLen);
            FrameData newFrame = new FrameData(this.isIFrame, newData, this.rawFrameData.length);
            res.add(newFrame);
        }
        return res;
    }
}
