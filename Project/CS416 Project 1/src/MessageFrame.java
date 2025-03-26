public class MessageFrame {
    private final Frame frame;

    public MessageFrame(Frame frame) {
        this.frame = frame;
    }

    public void addMessage(String message) {
        frame.data = message.getBytes();
    }

    public String getMessage() {
        return new String(frame.data);
    }
}
