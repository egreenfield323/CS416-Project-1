
import java.nio.ByteBuffer;


public class MessageFrame {
    private final Frame frame;

    public MessageFrame(Frame frame) {
        this.frame = frame;
    }

    public void addMessage(String message) {
        byte typeByte = (byte) Frame.FrameType.MESSAGE.ordinal();

        int length = message.getBytes().length + 1;
        byte[] data = new byte[length];

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.put(typeByte);
        buffer.put(message.getBytes());

        frame.data = message.getBytes();
    }

    public String getMessage() {
        ByteBuffer buffer = ByteBuffer.wrap(frame.data);
        buffer.get();

        byte[] messageBytes = new byte[frame.data.length - 1];
        buffer.get(messageBytes);

        return new String(messageBytes);
    }
}
