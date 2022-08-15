package au.id.jms.usbaudio;

public class UsbAudio {
    static {
        System.loadLibrary("usbaudio");
    }

    public native boolean setup(int fileDescriptor, int pid, int vid);
    public native boolean close();
    public native void loop();
    public native void stop();
    public native int measure();

}