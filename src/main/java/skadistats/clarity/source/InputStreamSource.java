package skadistats.clarity.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.exception.BytesNotReadException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

public class InputStreamSource extends Source {

    private static final Logger log = LoggerFactory.getLogger(InputStreamSource.class);

    private InputStream stream;
    private int position;
    private byte[] dummy = new byte[32768];

    private int lastOffset = -1;

    public InputStreamSource(InputStream stream) {
        this.stream = stream;
        this.position = 0;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int newPosition) throws IOException {
        if (position > newPosition) {
            throw new UnsupportedOperationException("cannot rewind input stream");
        }
        while (position != newPosition) {
            int r = Math.min(dummy.length, newPosition - position);
            try {
                readBytes(dummy, 0, r);
            }catch (BytesNotReadException ex){
                log.debug("Reached end of file. Save offsed counter and skip processing.");
            }
        }
    }

    @Override
    public byte readByte() throws IOException, BytesNotReadException {
        int i = stream.read();
        if (i == -1) {
            // get path to file though reflection and create new connection to file when file

            // reflection
            FileInputStream file = (FileInputStream) stream;
            Field field = null;
            try {
                field = file.getClass().getDeclaredField("path");
            }catch (NoSuchFieldException err){
                log.error("Missing path in file object.");
            }
            Object value = null;
            field.setAccessible(true);
            try {
                value = field.get(file);
            }catch (IllegalAccessException e){
                log.error("Cannot access 'path' varialble on file through reflection.");
            }
            String fileName = (String) value;

            // close old one and open new one (with more data)
            stream.close();
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName));
            InputStreamSource src = new InputStreamSource(bis);
            stream = src.stream;

            throw new BytesNotReadException();
        }
        position++;
        return (byte) i;
    }

    @Override
    public void readBytes(byte[] dest, int offset, int length) throws IOException, BytesNotReadException {
        if(lastOffset != -1){
            offset = lastOffset;
        }

        while (length > 0) {
            int r = stream.read(dest, offset, length);
            if (r == -1) {
                //throw new EOFException();
                // when on end of file does not throw exception but skip it and remember where it stopped
                lastOffset = offset;
                throw new BytesNotReadException();
            }else{
                lastOffset = -1;
            }
            position += r;
            offset += r;
            length -= r;
        }
    }

}
